package hub.support

import hub.operation.*
import hub.resource.*
import hub.resource.constraint.HalfOpenWindow
import hub.schedule.*
import hub.schedule.greedy.GreedySchedule
import cats.Id

import java.time.{Duration, LocalDateTime}

/**
 * School-timetable scenario builders for operation-graph scheduling tests.
 */
object OperationGraphScenarios:

  private val mondayEight: LocalDateTime = LocalDateTime.of(2026, 6, 1, 8, 0)

  def kind(name: String, hours: Int, requirement: ResourceRequirement = ResourceRequirement.Unrestricted): OperationKind =
    OperationKind(name, Duration.ofHours(hours), requirement)

  def resource(id: String, tags: Set[ResourceTag] = Set.empty): Resource =
    Resource(ResourceId.parse(id).fold(e => throw IllegalArgumentException(e), identity), tags)

  def tagged(id: String, tag: String): Resource =
    resource(id, Set(ResourceTag(tag)))

  final case class LinearPlanView(
      operations: List[Operation],
      plan: Plan,
      firstId: OperationId,
      secondId: OperationId,
      now: LocalDateTime,
  )

  def linearTwoSteps(): LinearPlanView =
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(algebra, kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Then(history)),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    LinearPlanView(ops, plan, algebra, history, mondayEight)

  final case class ForkPlanView(plan: Plan, mainId: OperationId, altId: OperationId)

  def oneOfPicksPriorityBranch(): ForkPlanView =
    val homeroom = OperationId.unsafe("7a/homeroom/1")
    val music = OperationId.unsafe("7a/music/1")
    val art = OperationId.unsafe("7a/art/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(homeroom, kind("homeroom", 1, ResourceRequirement.OneOf(List(room.id))), Successor.OneOf(List(music, art))),
        Operation(music, kind("music", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
        Operation(art, kind("art", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    ForkPlanView(plan, music, art)

  final case class ParallelPlanView(plan: Plan, leftId: OperationId, rightId: OperationId, parentEnd: LocalDateTime)

  def allSuccessorsOverlap(): ParallelPlanView =
    val assembly = OperationId.unsafe("school/assembly/1")
    val chemistry = OperationId.unsafe("7a/chemistry/1")
    val pe = OperationId.unsafe("7b/pe/1")
    val hall = resource("hall")
    val lab = resource("lab-chem")
    val gym = resource("gym")
    val ops =
      List(
        Operation(assembly, kind("assembly", 1, ResourceRequirement.OneOf(List(hall.id))), Successor.All(List(chemistry, pe))),
        Operation(chemistry, kind("chemistry", 2, ResourceRequirement.OneOf(List(lab.id))), Successor.Done),
        Operation(pe, kind("pe", 2, ResourceRequirement.OneOf(List(gym.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(ops, Nil, List(hall, lab, gym), mondayEight)
    val parentEnd = plan.at(assembly).map(_.end).getOrElse(mondayEight)
    ParallelPlanView(plan, chemistry, pe, parentEnd)

  final case class PrefixPlanView(plan: Plan, pendingId: OperationId, expectedStart: LocalDateTime)

  def completedPrefixSkipsFact(): PrefixPlanView =
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val room = resource("room-12")
    val factEnd = mondayEight.plusHours(3)
    val ops =
      List(
        Operation(
          algebra,
          kind("algebra", 3, ResourceRequirement.OneOf(List(room.id))),
          Successor.Then(history),
          Some(Interval(mondayEight, factEnd, Some(room.id))),
        ),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    PrefixPlanView(plan, history, factEnd)

  final case class SameIntervalView(plan: Plan, leftId: OperationId, rightId: OperationId)

  def sameIntervalSharesSlot(): SameIntervalView =
    val classA = OperationId.unsafe("7a/assembly/1")
    val classB = OperationId.unsafe("7b/assembly/1")
    val hall = resource("hall")
    val ops =
      List(
        Operation(classA, kind("assembly", 2, ResourceRequirement.OneOf(List(hall.id))), Successor.Done),
        Operation(classB, kind("assembly", 2, ResourceRequirement.OneOf(List(hall.id))), Successor.Done),
      )
    val constraints = List(Constraint.SameInterval(List(classA, classB), Some(mondayEight)))
    val plan = GreedySchedule.live[Id].plan(ops, constraints, List(hall), mondayEight)
    SameIntervalView(plan, classA, classB)

  final case class DowntimePlanView(plan: Plan, opId: OperationId, downtimeEnd: LocalDateTime)

  def respectsResourceDowntime(): DowntimePlanView =
    val pe = OperationId.unsafe("7a/pe/1")
    val closedUntil = mondayEight.plusHours(2)
    val gym = resource("gym")
    val ops =
      List(Operation(pe, kind("pe", 1, ResourceRequirement.OneOf(List(gym.id))), Successor.Done))
    val constraints = List(Constraint.Forbidden(gym.id, List(HalfOpenWindow(mondayEight, closedUntil))))
    val plan = GreedySchedule.live[Id].plan(ops, constraints, List(gym), mondayEight)
    DowntimePlanView(plan, pe, closedUntil)

  final case class CapacityPlanView(plan: Plan, leftId: OperationId, rightId: OperationId)

  def capacityAllowsOverlap(): CapacityPlanView =
    val classA = OperationId.unsafe("7a/pe/1")
    val classB = OperationId.unsafe("7b/pe/1")
    val gym = resource("gym")
    val ops =
      List(
        Operation(classA, kind("pe", 2, ResourceRequirement.OneOf(List(gym.id))), Successor.Done),
        Operation(classB, kind("pe", 2, ResourceRequirement.OneOf(List(gym.id))), Successor.Done),
      )
    val constraints = List(Constraint.Capacity(gym.id, 2))
    val plan = GreedySchedule.live[Id].plan(ops, constraints, List(gym), mondayEight)
    CapacityPlanView(plan, classA, classB)

  final case class CompositeDayView(
      plan: Plan,
      mondayId: OperationId,
      algebraId: OperationId,
      historyId: OperationId,
  )

  def mondayCompositeDay(): CompositeDayView =
    val monday = OperationId.unsafe("7a/monday/1")
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(monday, Body.Composite(List(algebra, history)), Successor.Done),
        Operation(algebra, kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Then(history)),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    CompositeDayView(plan, monday, algebra, history)

  final case class CompositeThenView(
      plan: Plan,
      mondayId: OperationId,
      tuesdayId: OperationId,
  )

  def mondayThenTuesday(): CompositeThenView =
    val monday = OperationId.unsafe("7a/monday/1")
    val tuesday = OperationId.unsafe("7a/tuesday/1")
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(monday, Body.Composite(List(algebra, history)), Successor.Then(tuesday)),
        Operation(algebra, kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Then(history)),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
        Operation(tuesday, kind("homeroom", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    CompositeThenView(plan, monday, tuesday)

  final case class NestedCompositeView(
      plan: Plan,
      weekId: OperationId,
      mondayId: OperationId,
      algebraId: OperationId,
      historyId: OperationId,
  )

  def weekContainsMonday(): NestedCompositeView =
    val week = OperationId.unsafe("7a/week/1")
    val monday = OperationId.unsafe("7a/monday/1")
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(week, Body.Composite(List(monday)), Successor.Done),
        Operation(monday, Body.Composite(List(algebra, history)), Successor.Done),
        Operation(algebra, kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Then(history)),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    NestedCompositeView(plan, week, monday, algebra, history)

  final case class CompositeActualView(plan: Plan, mondayId: OperationId, algebraId: OperationId, historyId: OperationId)

  def compositeActualSkipsParts(): CompositeActualView =
    val monday = OperationId.unsafe("7a/monday/1")
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val room = resource("room-12")
    val factEnd = mondayEight.plusHours(3)
    val ops =
      List(
        Operation(
          monday,
          Body.Composite(List(algebra, history)),
          Successor.Done,
          Some(Interval(mondayEight, factEnd, None)),
        ),
        Operation(algebra, kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Then(history)),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    CompositeActualView(plan, monday, algebra, history)

  final case class CompositePartFactView(plan: Plan, mondayId: OperationId, algebraId: OperationId, historyId: OperationId)

  def compositeEnvelopeIncludesPartFact(): CompositePartFactView =
    val monday = OperationId.unsafe("7a/monday/1")
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val room = resource("room-12")
    val factEnd = mondayEight.plusHours(2)
    val ops =
      List(
        Operation(monday, Body.Composite(List(algebra, history)), Successor.Done),
        Operation(
          algebra,
          kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))),
          Successor.Then(history),
          Some(Interval(mondayEight, factEnd, Some(room.id))),
        ),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    CompositePartFactView(plan, monday, algebra, history)

  def emptyCompositeParts(): Unit =
    val monday = OperationId.unsafe("7a/monday/1")
    val room = resource("room-12")
    val ops = List(Operation(monday, Body.Composite(Nil), Successor.Done))
    GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    ()

  def compositeCycle(): Unit =
    val week = OperationId.unsafe("7a/week/1")
    val monday = OperationId.unsafe("7a/monday/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(week, Body.Composite(List(monday)), Successor.Done),
        Operation(monday, Body.Composite(List(week)), Successor.Done),
      )
    GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    ()

  def sharedCompositePart(): Unit =
    val monday = OperationId.unsafe("7a/monday/1")
    val tuesday = OperationId.unsafe("7a/tuesday/1")
    val algebra = OperationId.unsafe("7a/algebra/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(monday, Body.Composite(List(algebra)), Successor.Done),
        Operation(tuesday, Body.Composite(List(algebra)), Successor.Done),
        Operation(algebra, kind("algebra", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    ()

  def partSuccessorLeavesComposite(): Unit =
    val monday = OperationId.unsafe("7a/monday/1")
    val algebra = OperationId.unsafe("7a/algebra/1")
    val tuesday = OperationId.unsafe("7a/tuesday/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(monday, Body.Composite(List(algebra)), Successor.Done),
        Operation(algebra, kind("algebra", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Then(tuesday)),
        Operation(tuesday, kind("homeroom", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    ()

  def sameIntervalOnComposite(): Unit =
    val monday = OperationId.unsafe("7a/monday/1")
    val algebra = OperationId.unsafe("7a/algebra/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(monday, Body.Composite(List(algebra)), Successor.Done),
        Operation(algebra, kind("algebra", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val constraints = List(Constraint.SameInterval(List(monday), Some(mondayEight)))
    GreedySchedule.live[Id].plan(ops, constraints, List(room), mondayEight)
    ()

  final case class WarmReuseView(first: Plan, second: Plan, algebraId: OperationId, historyId: OperationId)

  def warmStartReusesFeasiblePrior(): WarmReuseView =
    val view = linearTwoSteps()
    val second =
      GreedySchedule.live[Id].plan(view.operations, Nil, List(resource("room-12")), view.now, view.plan)
    WarmReuseView(view.plan, second, view.firstId, view.secondId)

  final case class WarmFactView(priorHistoryStart: LocalDateTime, plan: Plan, historyId: OperationId)

  def warmStartKeepsHistoryAfterAlgebraFact(): WarmFactView =
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(algebra, kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Then(history)),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val prior = GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    val historyStart = prior.at(history).map(_.start).getOrElse(mondayEight)
    val algebraSlot = prior.at(algebra).get
    val withFact =
      List(
        Operation(
          algebra,
          kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))),
          Successor.Then(history),
          Some(algebraSlot),
        ),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(withFact, Nil, List(room), mondayEight, prior)
    WarmFactView(historyStart, plan, history)

  final case class WarmForbiddenView(
      priorAlgebraStart: LocalDateTime,
      priorHistoryStart: LocalDateTime,
      plan: Plan,
      algebraId: OperationId,
      historyId: OperationId,
  )

  def warmStartShiftsHistoryUnderForbidden(): WarmForbiddenView =
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val room = resource("room-12")
    val ops =
      List(
        Operation(algebra, kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Then(history)),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val prior = GreedySchedule.live[Id].plan(ops, Nil, List(room), mondayEight)
    val algebraStart = prior.at(algebra).map(_.start).getOrElse(mondayEight)
    val historyStart = prior.at(history).map(_.start).getOrElse(mondayEight)
    val blocked = HalfOpenWindow(historyStart, historyStart.plusHours(1))
    val constraints = List(Constraint.Forbidden(room.id, List(blocked)))
    val plan = GreedySchedule.live[Id].plan(ops, constraints, List(room), mondayEight, prior)
    WarmForbiddenView(algebraStart, historyStart, plan, algebra, history)

  final case class WarmRemovedView(plan: Plan, removedId: OperationId, keptId: OperationId)

  def warmStartDropsRemovedNode(): WarmRemovedView =
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val room = resource("room-12")
    val full =
      List(
        Operation(algebra, kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Then(history)),
        Operation(history, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val prior = GreedySchedule.live[Id].plan(full, Nil, List(room), mondayEight)
    val skipped =
      List(
        Operation(algebra, kind("algebra", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(skipped, Nil, List(room), mondayEight, prior)
    WarmRemovedView(plan, history, algebra)

  final case class WarmRepeatView(
      priorDecimalsStart: LocalDateTime,
      plan: Plan,
      repeatId: OperationId,
      decimalsId: OperationId,
  )

  def warmStartPlacesRepeatBeforeKeptTail(): WarmRepeatView =
    val fractions = OperationId.unsafe("7a/fractions/1")
    val decimals = OperationId.unsafe("7a/decimals/1")
    val room = resource("room-12")
    val baseline =
      List(
        Operation(fractions, kind("fractions", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Then(decimals)),
        Operation(decimals, kind("decimals", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val prior = GreedySchedule.live[Id].plan(baseline, Nil, List(room), mondayEight)
    val decimalsStart = prior.at(decimals).map(_.start).getOrElse(mondayEight)
    val fractionsSlot = prior.at(fractions).get
    val repeat = OperationId.unsafe("7a/fractions/2")
    val revised =
      List(
        Operation(
          fractions,
          kind("fractions", 2, ResourceRequirement.OneOf(List(room.id))),
          Successor.Then(repeat),
          Some(fractionsSlot),
        ),
        Operation(repeat, kind("fractions", 2, ResourceRequirement.OneOf(List(room.id))), Successor.Then(decimals)),
        Operation(decimals, kind("decimals", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done),
      )
    val plan = GreedySchedule.live[Id].plan(revised, Nil, List(room), mondayEight, prior)
    WarmRepeatView(decimalsStart, plan, repeat, decimals)

  final case class OccupiedDeferView(firstEnd: LocalDateTime, secondStart: LocalDateTime)

  def occupiedDefersSecondGraph(): OccupiedDeferView =
    val firstId = OperationId.unsafe("7a/algebra/1")
    val secondId = OperationId.unsafe("7b/history/1")
    val room = resource("room-12")
    val first =
      List(Operation(firstId, kind("algebra", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done, None))
    val second =
      List(Operation(secondId, kind("history", 1, ResourceRequirement.OneOf(List(room.id))), Successor.Done, None))
    val planA = GreedySchedule.live[Id].plan(first, Nil, List(room), mondayEight)
    val planB =
      GreedySchedule.live[Id].plan(second, Nil, List(room), mondayEight, prior = Plan.empty, occupied = planA)
    OccupiedDeferView(
      planA.at(firstId).map(_.end).getOrElse(mondayEight),
      planB.at(secondId).map(_.start).getOrElse(mondayEight),
    )

end OperationGraphScenarios
