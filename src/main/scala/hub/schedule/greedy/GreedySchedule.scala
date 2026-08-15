package hub.schedule.greedy

import cats.Applicative
import cats.syntax.all.*
import hub.operation.*
import hub.resource.{Resource, ResourceId, Resources}
import hub.schedule.*

import java.time.{Duration, LocalDateTime}
import scala.collection.mutable

/**
 * Wave-based greedy [[hub.schedule.Schedule]] over an operation graph.
 *
 * Walks active nodes (honoring [[Successor.OneOf]] selection), skips nodes with
 * [[Operation.actual]], seeds feasible rows from [[prior]], places
 * [[Constraint.SameInterval]] groups as one shared slot, seals
 * [[Body.Composite]] envelopes without resource occupancy, and books atomic
 * occupancy against [[Constraint.Capacity]] and [[Constraint.Forbidden]] via
 * [[ResourceSlot]].
 *
 * {{{
 * GreedySchedule.live[IO].plan(lessons, constraints, rooms, mondayEight)
 * GreedySchedule.live[IO].plan(lessons, constraints, rooms, mondayEight, lastPlan)
 * }}}
 */
final class GreedySchedule[F[_]: Applicative] extends Schedule[F]:

  /** See [[Schedule.plan]] */
  def plan(
      operations: List[Operation],
      constraints: List[Constraint],
      resources: List[Resource],
      now: LocalDateTime,
      prior: Plan = Plan.empty,
      occupied: Plan = Plan.empty,
  ): F[Plan] =
    Plan(GreedySchedule.place(operations, constraints, resources, now, prior, occupied)).pure[F]

object GreedySchedule:
  /** Builds a scheduler for effect type `F` */
  def live[F[_]: Applicative]: GreedySchedule[F] =
    new GreedySchedule[F]

  private final case class Placement(
      start: LocalDateTime,
      end: LocalDateTime,
      resource: Option[ResourceId],
      booked: List[ResourceId] = Nil,
  ):
    def resources: List[ResourceId] =
      if booked.nonEmpty then booked else resource.toList

  private final case class World(
      capacity: Map[ResourceId, Int],
      forbidden: Map[ResourceId, List[hub.resource.constraint.HalfOpenWindow]],
  )

  private final case class Nesting(
      parts: Map[OperationId, List[OperationId]],
      owner: Map[OperationId, OperationId],
  )

  /**
   * Pure placement: returns planned intervals only (facts stay on operations).
   *
   * @param operations graph to schedule
   * @param constraints resource world and SameInterval coupling
   * @param resources catalog identity used for eligibility
   * @param now ready floor for roots
   * @param prior previous plan; feasible atomic rows are kept when still valid
   * @param occupied foreign occupancy booked on resources without placing ids
   */
  def place(
      operations: List[Operation],
      constraints: List[Constraint],
      resources: List[Resource],
      now: LocalDateTime,
      prior: Plan = Plan.empty,
      occupied: Plan = Plan.empty,
  ): Map[OperationId, Interval] =
    val byId = operations.map(op => op.id -> op).toMap
    val nesting = nestingOf(operations, byId)
    validate(operations, constraints, byId, nesting)
    val preds = predecessors(operations)
    val skipped = skippedParts(operations, nesting)
    val active = activeIds(operations, byId) -- skipped
    val sameGroups = constraints.collect:
      case Constraint.SameInterval(ops, minReady) => (ops.filter(active.contains), minReady)
    .filter(_._1.nonEmpty)
    val sameMembers = sameGroups.flatMap(_._1).toSet
    val world = worldOf(constraints)
    val catalog = Resources(resources)
    val occupancy = mutable.Map.empty[ResourceId, List[(LocalDateTime, LocalDateTime)]]
    val planned = mutable.Map.empty[OperationId, Placement]
    val completed = mutable.Map.empty[OperationId, Placement]
    seedActuals(operations, occupancy, completed)
    seedOccupied(occupied, occupancy)
    seedPrior(
      prior,
      byId,
      active,
      sameMembers,
      catalog,
      occupancy,
      planned,
      completed,
      preds,
      nesting,
      world,
      now,
    )
    var guard = 0
    var progressed = true
    while progressed && guard < operations.length * 2 + 8 do
      guard += 1
      progressed = false
      val readyAtoms =
        active
          .filter(id => byId.get(id).exists(_.body match
            case Body.Atom(_) => true
            case _            => false))
          .filterNot(id => planned.contains(id) || completed.contains(id))
          .filter(id => predecessorsReady(id, preds, nesting, planned, completed))
          .toList
          .sortBy(_.text)
      val pendingGroups =
        sameGroups.filter: (ops, _) =>
          ops.forall(id => readyAtoms.contains(id) || planned.contains(id) || completed.contains(id)) &&
            ops.exists(id => !planned.contains(id) && !completed.contains(id))
      if pendingGroups.nonEmpty then
        val (ops, minReady) =
          pendingGroups
            .map((ids, readyAt) => (ids.filter(id => !planned.contains(id) && !completed.contains(id)), readyAt))
            .filter(_._1.nonEmpty)
            .minBy(_._1.map(_.text).mkString)
        placeSameInterval(ops, minReady, byId, catalog, occupancy, planned, now, preds, nesting, completed, world)
        progressed = true
      else if readyAtoms.nonEmpty then
        placeOne(readyAtoms.head, byId, catalog, occupancy, planned, now, preds, nesting, completed, world)
        progressed = true
      else if sealReadyComposites(operations, nesting, planned, completed) then
        progressed = true
    planned.view.mapValues(p => Interval(p.start, p.end, p.resource, p.booked)).toMap

  private def nestingOf(operations: List[Operation], byId: Map[OperationId, Operation]): Nesting =
    val parts = operations.collect:
      case Operation(id, Body.Composite(children), _, _) => id -> children
    .toMap
    val owner = mutable.Map.empty[OperationId, OperationId]
    parts.foreach: (parent, children) =>
      children.foreach: child =>
        owner.get(child).foreach: other =>
          throw IllegalArgumentException(s"operation ${child.text} belongs to both ${other.text} and ${parent.text}")
        if !byId.contains(child) then
          throw IllegalArgumentException(s"composite ${parent.text} references missing part ${child.text}")
        owner(child) = parent
    Nesting(parts, owner.toMap)

  private def validate(
      operations: List[Operation],
      constraints: List[Constraint],
      byId: Map[OperationId, Operation],
      nesting: Nesting,
  ): Unit =
    nesting.parts.foreach: (parent, children) =>
      if children.isEmpty then
        throw IllegalArgumentException(s"composite ${parent.text} has empty parts")
    detectCycles(nesting)
    nesting.parts.foreach: (parent, children) =>
      val sibling = children.toSet
      children.foreach: child =>
        byId.get(child).foreach: op =>
          successorIds(op).foreach: next =>
            if !sibling.contains(next) then
              throw IllegalArgumentException(
                s"part ${child.text} successor ${next.text} leaves composite ${parent.text}"
              )
    constraints.foreach:
      case Constraint.SameInterval(ops, _) =>
        ops.foreach: id =>
          byId.get(id).foreach:
            case Operation(_, Body.Composite(_), _, _) =>
              throw IllegalArgumentException(s"SameInterval cannot include composite ${id.text}")
            case _ => ()
      case _ => ()

  private def detectCycles(nesting: Nesting): Unit =
    def visit(id: OperationId, stack: Set[OperationId]): Unit =
      if stack.contains(id) then
        throw IllegalArgumentException(s"composite nesting cycle at ${id.text}")
      nesting.parts.getOrElse(id, Nil).foreach(child => visit(child, stack + id))
    nesting.parts.keys.foreach(id => visit(id, Set.empty))

  private def skippedParts(operations: List[Operation], nesting: Nesting): Set[OperationId] =
    val closed =
      operations.collect:
        case Operation(id, Body.Composite(_), _, Some(_)) => id
      .toSet
    def descendants(id: OperationId): Set[OperationId] =
      nesting.parts.getOrElse(id, Nil).toSet.flatMap: child =>
        descendants(child) + child
    closed.flatMap(descendants)

  private def worldOf(constraints: List[Constraint]): World =
    val capacities = constraints.collect:
      case Constraint.Capacity(id, max) =>
        if max < 1 then
          throw IllegalArgumentException(s"Constraint.Capacity max must be at least 1 for ${id.value}, got $max")
        id -> max
    val grouped = capacities.groupBy(_._1)
    grouped.find(_._2.size > 1).foreach: (id, _) =>
      throw IllegalArgumentException(s"duplicate Constraint.Capacity for ${id.value}")
    val forbidden = constraints
      .collect:
        case Constraint.Forbidden(id, windows) => id -> windows
      .groupBy(_._1)
      .view
      .mapValues(_.flatMap(_._2))
      .toMap
    World(capacities.toMap, forbidden)

  private def seedActuals(
      operations: List[Operation],
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      completed: mutable.Map[OperationId, Placement],
  ): Unit =
    operations.foreach: op =>
      op.actual.foreach: interval =>
        completed(op.id) = Placement(interval.start, interval.end, interval.resource, interval.booked)
        bookAll(occupancy, interval.resources, interval.start, interval.end)

  /**
   * Books foreign resource intervals without placing those operation ids.
   */
  private def seedOccupied(
      occupied: Plan,
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
  ): Unit =
    occupied.intervals.values.foreach: interval =>
      if interval.start.isBefore(interval.end) then
        bookAll(occupancy, interval.resources, interval.start, interval.end)

  /**
   * Keeps feasible atomic rows from [[prior]] without shifting them.
   *
   * Composites and SameInterval members are not seeded; the main wave places them.
   */
  private def seedPrior(
      prior: Plan,
      byId: Map[OperationId, Operation],
      active: Set[OperationId],
      sameMembers: Set[OperationId],
      catalog: Resources,
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      planned: mutable.Map[OperationId, Placement],
      completed: mutable.Map[OperationId, Placement],
      preds: Map[OperationId, List[OperationId]],
      nesting: Nesting,
      world: World,
      now: LocalDateTime,
  ): Unit =
    val rows = prior.intervals.toList.sortBy((id, interval) => (interval.start, id.text))
    var guard = 0
    var progressed = true
    while progressed && guard < rows.length + 2 do
      guard += 1
      progressed = false
      rows.foreach: (id, interval) =>
        if !planned.contains(id) && !completed.contains(id) &&
          keepPrior(
            id,
            interval,
            byId,
            active,
            sameMembers,
            catalog,
            occupancy,
            planned,
            completed,
            preds,
            nesting,
            world,
            now,
          )
        then
          planned(id) = Placement(interval.start, interval.end, interval.resource, interval.booked)
          bookAll(occupancy, interval.resources, interval.start, interval.end)
          progressed = true

  private def keepPrior(
      id: OperationId,
      interval: Interval,
      byId: Map[OperationId, Operation],
      active: Set[OperationId],
      sameMembers: Set[OperationId],
      catalog: Resources,
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      planned: mutable.Map[OperationId, Placement],
      completed: mutable.Map[OperationId, Placement],
      preds: Map[OperationId, List[OperationId]],
      nesting: Nesting,
      world: World,
      now: LocalDateTime,
  ): Boolean =
    if !active.contains(id) || sameMembers.contains(id) then false
    else
      byId.get(id) match
        case Some(op @ Operation(_, Body.Atom(kind), _, None)) =>
          if !predecessorsReady(id, preds, nesting, planned, completed) then false
          else
            val ready = readyFloor(id, preds, nesting, planned, completed, now)
            val duration = normalized(kind.normDuration)
            if interval.start.isBefore(ready) || !interval.end.equals(interval.start.plus(duration)) then false
            else
              val combos = OperationEligibility.groups(op, catalog).map(_.map(_.id).toSet)
              val booked = interval.resources.toSet
              if booked.isEmpty then combos.isEmpty
              else
                combos.exists(_ == booked) &&
                interval.resources.forall(rid => slotHolds(rid, interval.start, duration, occupancy, world))
        case _ => false

  private def slotHolds(
      resource: ResourceId,
      start: LocalDateTime,
      duration: Duration,
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      world: World,
  ): Boolean =
    ResourceSlot.start(
      resource,
      start,
      duration,
      occupancy.getOrElse(resource, Nil),
      world.forbidden.getOrElse(resource, Nil),
      world.capacity.getOrElse(resource, 1),
    ) == start

  private def predecessors(operations: List[Operation]): Map[OperationId, List[OperationId]] =
    val acc = mutable.Map.empty[OperationId, List[OperationId]]
    operations.foreach: op =>
      successorIds(op).foreach: next =>
        acc(next) = acc.getOrElse(next, Nil) :+ op.id
    operations.map(_.id).map(id => id -> acc.getOrElse(id, Nil)).toMap

  private def successorIds(op: Operation): List[OperationId] =
    op.successor match
      case Successor.Done        => Nil
      case Successor.Then(next)  => List(next)
      case Successor.All(next)   => next
      case Successor.OneOf(next) => next

  private def activeIds(operations: List[Operation], byId: Map[OperationId, Operation]): Set[OperationId] =
    operations.iterator
      .map(_.id)
      .filter: id =>
        val parents = operations.filter(op => successorIds(op).contains(id))
        parents.isEmpty || parents.exists: parent =>
          parent.successor match
            case Successor.OneOf(next) => pickOneOf(next, byId).contains(id)
            case _                     => true
      .toSet

  private def pickOneOf(next: List[OperationId], byId: Map[OperationId, Operation]): Option[OperationId] =
    val withActual = next.find(id => byId.get(id).exists(_.actual.nonEmpty))
    withActual.orElse(next.headOption)

  private def effectivePreds(
      id: OperationId,
      preds: Map[OperationId, List[OperationId]],
      nesting: Nesting,
  ): List[OperationId] =
    val base = preds.getOrElse(id, Nil)
    nesting.owner.get(id) match
      case Some(parent) =>
        val siblings = nesting.parts.getOrElse(parent, Nil).toSet
        if base.exists(siblings.contains) then base
        else base ++ preds.getOrElse(parent, Nil)
      case None => base

  private def predecessorsReady(
      id: OperationId,
      preds: Map[OperationId, List[OperationId]],
      nesting: Nesting,
      planned: mutable.Map[OperationId, Placement],
      completed: mutable.Map[OperationId, Placement],
  ): Boolean =
    effectivePreds(id, preds, nesting).forall(pred => planned.contains(pred) || completed.contains(pred))

  private def readyFloor(
      id: OperationId,
      preds: Map[OperationId, List[OperationId]],
      nesting: Nesting,
      planned: mutable.Map[OperationId, Placement],
      completed: mutable.Map[OperationId, Placement],
      now: LocalDateTime,
  ): LocalDateTime =
    val ends =
      effectivePreds(id, preds, nesting).flatMap: pred =>
        planned.get(pred).orElse(completed.get(pred)).map(_.end)
    (now :: ends).max

  private def sealReadyComposites(
      operations: List[Operation],
      nesting: Nesting,
      planned: mutable.Map[OperationId, Placement],
      completed: mutable.Map[OperationId, Placement],
  ): Boolean =
    val candidates =
      nesting.parts.toList
        .filter: (parent, children) =>
          !planned.contains(parent) && !completed.contains(parent) &&
            children.forall(child => planned.contains(child) || completed.contains(child))
        .sortBy(_._1.text)
    candidates.headOption match
      case Some((parent, children)) =>
        val slots =
          children.flatMap: child =>
            planned.get(child).orElse(completed.get(child))
        val start = slots.map(_.start).min
        val end = slots.map(_.end).max
        planned(parent) = Placement(start, end, None)
        true
      case None => false

  private def placeOne(
      id: OperationId,
      byId: Map[OperationId, Operation],
      catalog: Resources,
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      planned: mutable.Map[OperationId, Placement],
      now: LocalDateTime,
      preds: Map[OperationId, List[OperationId]],
      nesting: Nesting,
      completed: mutable.Map[OperationId, Placement],
      world: World,
  ): Unit =
    byId.get(id).foreach: op =>
      if !planned.contains(id) && !completed.contains(id) then
        op.body match
          case Body.Atom(_) =>
            val ready = readyFloor(id, preds, nesting, planned, completed, now)
            val placed = placeOp(op, ready, catalog, occupancy, world)
            planned(id) = placed
            bookAll(occupancy, placed.resources, placed.start, placed.end)
          case Body.Composite(_) => ()

  private def placeSameInterval(
      ops: List[OperationId],
      minReady: Option[LocalDateTime],
      byId: Map[OperationId, Operation],
      catalog: Resources,
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      planned: mutable.Map[OperationId, Placement],
      now: LocalDateTime,
      preds: Map[OperationId, List[OperationId]],
      nesting: Nesting,
      completed: mutable.Map[OperationId, Placement],
      world: World,
  ): Unit =
    val members = ops.flatMap(byId.get)
    if members.nonEmpty then
      val floors = ops.map(id => readyFloor(id, preds, nesting, planned, completed, now))
      val ready = (minReady.toList ++ floors).max
      val duration =
        members
          .map:
            case Operation(_, Body.Atom(kind), _, _) => kind.normDuration
            case _                                   => Duration.ofNanos(1L)
          .max
      val candidates =
        members
          .map(op => OperationEligibility.candidates(op, catalog).map(_.id).toSet)
          .reduceOption(_ intersect _)
          .getOrElse(Set.empty)
          .toList
          .map(id => List(catalog.get(id)))
          .sortBy(_.head.id.value)
      val (ids, start) = pickSlot(candidates, ready, duration, occupancy, world)
      val end = start.plus(duration)
      val placement = Placement(start, end, ids.headOption, ids)
      bookAll(occupancy, ids, start, end)
      ops.foreach(id => planned(id) = placement)

  private def placeOp(
      op: Operation,
      ready: LocalDateTime,
      catalog: Resources,
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      world: World,
  ): Placement =
    op.body match
      case Body.Atom(kind) =>
        val duration = normalized(kind.normDuration)
        val combos = OperationEligibility.groups(op, catalog)
        val (ids, start) = pickSlot(combos, ready, duration, occupancy, world)
        Placement(start, start.plus(duration), ids.headOption, ids)
      case Body.Composite(_) =>
        Placement(ready, ready, None)

  private def pickSlot(
      combos: List[List[Resource]],
      ready: LocalDateTime,
      duration: Duration,
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      world: World,
  ): (List[ResourceId], LocalDateTime) =
    if combos.isEmpty then (Nil, ready)
    else
      val ranked =
        combos.map: combo =>
          val ids = combo.map(_.id)
          (ids, multiStart(ids, ready, duration, occupancy, world))
      val chosen = ranked.minBy((ids, start) => (start, ids.map(_.value).mkString("|")))
      chosen

  private def multiStart(
      ids: List[ResourceId],
      ready: LocalDateTime,
      duration: Duration,
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      world: World,
  ): LocalDateTime =
    if ids.isEmpty then ready
    else
      var candidate = ready
      var guard = 0
      while guard < 1000 do
        guard += 1
        val starts =
          ids.map: id =>
            ResourceSlot.start(
              id,
              candidate,
              duration,
              occupancy.getOrElse(id, Nil),
              world.forbidden.getOrElse(id, Nil),
              world.capacity.getOrElse(id, 1),
            )
        val sync = starts.max
        if starts.forall(_ == sync) then return sync
        if !sync.isAfter(candidate) then return sync
        candidate = sync
      candidate

  private def bookAll(
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      ids: List[ResourceId],
      start: LocalDateTime,
      end: LocalDateTime,
  ): Unit =
    ids.foreach(id => book(occupancy, id, start, end))

  private def book(
      occupancy: mutable.Map[ResourceId, List[(LocalDateTime, LocalDateTime)]],
      id: ResourceId,
      start: LocalDateTime,
      end: LocalDateTime,
  ): Unit =
    occupancy(id) = occupancy.getOrElse(id, Nil) :+ ((start, end))

  private def normalized(duration: Duration): Duration =
    if duration.isNegative || duration.isZero then Duration.ofNanos(1L) else duration

end GreedySchedule
