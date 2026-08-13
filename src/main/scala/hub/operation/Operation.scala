package hub.operation

import cats.Show
import cats.syntax.show.*
import hub.resource.ResourceRequirement

import java.time.Duration

/**
 * Type of work that can be scheduled: catalog name, normative duration, and
 * resource need.
 *
 * Duration drives placement length. [[requirement]] selects eligible resources.
 * How those resources may be used in a run is [[hub.schedule.Constraint]].
 *
 * {{{
 * OperationKind("algebra", Duration.ofMinutes(45), ResourceRequirement.Unrestricted)
 * OperationKind("chemistry", Duration.ofHours(2), ResourceRequirement.AnyTagged(ResourceTag("lab")))
 * }}}
 *
 * @param name         human-readable kind name
 * @param normDuration planned wall-time length of one execution
 * @param requirement  which resources may run this kind
 */
case class OperationKind(
    name: OperationName,
    normDuration: Duration,
    requirement: ResourceRequirement,
)

object OperationKind:
  given Show[OperationKind] = Show.show: kind =>
    s"${kind.name}(norm=${kind.normDuration}, requirement=${kind.requirement})"

/**
 * Payload of one [[Operation]]: either a schedulable atom or a named container.
 *
 * {{{
 * Body.Atom(algebra)
 * Body.Composite(List(algebraId, historyId))
 * }}}
 */
enum Body:
  /** Leaf lesson with duration and resource need */
  case Atom(kind: OperationKind)

  /**
   * Named group of child operations.
   *
   * The container has no duration or resource need. Its plan interval is the
   * envelope of [[parts]]. Occupancy is booked only by atomic parts.
   */
  case Composite(parts: List[OperationId])

end Body

object Body:
  given Show[Body] = Show.show:
    case Body.Atom(kind)          => s"Atom(${kind.show})"
    case Body.Composite(parts)    => s"Composite(${parts.map(_.text).mkString(",")})"

/**
 * Schedulable graph node: the single object the planner places.
 *
 * An operation is identified by [[id]], described by [[body]], linked by
 * [[successor]], and optionally already executed via [[actual]]. When `actual`
 * is set, the greedy scheduler books that interval as occupancy and does not
 * emit a plan row for the node. A composite with `actual` also skips planning
 * of its parts.
 *
 * {{{
 * Operation(algebraId, algebra, Successor.Then(historyId), None)
 * Operation(mondayId, Body.Composite(List(algebraId, historyId)), Successor.Then(tuesdayId))
 * Operation(algebraId, algebra, Successor.Done, Some(Interval(start, end, Some(room12))))
 * }}}
 *
 * @param id        stable opaque key
 * @param body      atom kind or composite parts
 * @param successor outgoing graph edges
 * @param actual    closed execution fact, if any
 */
case class Operation(
    id: OperationId,
    body: Body,
    successor: Successor,
    actual: Option[Interval] = None,
)

object Operation:
  /**
   * Builds an atomic operation (compatibility with callers that pass a kind).
   *
   * {{{
   * Operation(algebraId, algebra, Successor.Then(historyId))
   * }}}
   */
  def apply(
      id: OperationId,
      kind: OperationKind,
      successor: Successor,
      actual: Option[Interval],
  ): Operation =
    Operation(id, Body.Atom(kind), successor, actual)

  /** Atomic operation with no closed fact */
  def apply(id: OperationId, kind: OperationKind, successor: Successor): Operation =
    apply(id, kind, successor, None)

  given Show[Operation] = Show.show: op =>
    s"Operation(id=${op.id.text}, body=${op.body.show}, successor=${op.successor}, actual=${op.actual})"

end Operation
