package hub.projection

import hub.operation.{Interval, Operation}
import hub.schedule.Plan

/**
 * Read-side merge of an operation graph and a disposable [[Plan]].
 *
 * Presentation (JSON, CSV) stays in the caller; this object
 * only forms timelines and coverage over hub types.
 *
 * {{{
 * val rows = Projections.timeline(lessons, timetable)
 * val left = Projections.pending(lessons, timetable)
 * }}}
 */
object Projections:

  /**
   * One timeline row: operation plus factual and/or planned interval.
   *
   * @param operation graph node
   * @param actual    copy of `operation.actual`
   * @param planned   interval from the plan, if any
   */
  final case class TimelineRow(
      operation: Operation,
      actual: Option[Interval],
      planned: Option[Interval],
  )

  /** Timeline rows sorted by operation id text */
  def timeline(operations: List[Operation], plan: Plan): List[TimelineRow] =
    operations
      .sortBy(_.id.text)
      .map: op =>
        TimelineRow(op, op.actual, plan.at(op.id))

  /** Operations without a fact and without a plan row */
  def pending(operations: List[Operation], plan: Plan): List[Operation] =
    operations.filter: op =>
      op.actual.isEmpty && plan.at(op.id).isEmpty

  /** True when every operation has either a fact or a planned interval */
  def coverage(operations: List[Operation], plan: Plan): Boolean =
    operations.forall: op =>
      op.actual.nonEmpty || plan.at(op.id).nonEmpty

end Projections
