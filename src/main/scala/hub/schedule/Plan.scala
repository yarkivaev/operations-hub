package hub.schedule

import hub.operation.{Interval, OperationId}
import hub.resource.ResourceId

/**
 * Disposable assignment of wall-time intervals to remaining operations.
 *
 * Contains only planned rows: operations with [[hub.operation.Operation.actual]]
 * are not duplicated here. Read models merge graph + plan via
 * [[hub.projection.Projections]].
 *
 * {{{
 * plan.at(algebraId)
 * plan.intervals
 * plan.byResource(room12)
 * }}}
 */
trait Plan:
  /** Planned interval for one operation, if scheduled */
  def at(id: OperationId): Option[Interval]

  /** All planned intervals keyed by operation id */
  def intervals: Map[OperationId, Interval]

  /** Planned intervals assigned to `resource`, ordered by start then id */
  def byResource(resource: ResourceId): List[(OperationId, Interval)]

object Plan:
  /** Builds an immutable plan from a map of intervals */
  def apply(rows: Map[OperationId, Interval]): Plan =
    new Plan:
      def at(id: OperationId): Option[Interval] = rows.get(id)
      def intervals: Map[OperationId, Interval] = rows
      def byResource(resource: ResourceId): List[(OperationId, Interval)] =
        rows.toList
          .filter((_, interval) => interval.resource.contains(resource))
          .sortBy((id, interval) => (interval.start, id.text))

  /** Empty plan with no assignments */
  val empty: Plan = apply(Map.empty)

end Plan
