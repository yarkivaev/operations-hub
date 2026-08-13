package hub.schedule

import hub.operation.OperationId
import hub.resource.ResourceId
import hub.resource.constraint.HalfOpenWindow

import java.time.LocalDateTime

/**
 * Rule for one planning run that is not encoded in the operation graph.
 *
 * Two families:
 *  - '''resource''' — world facts about a catalog id ([[Capacity]], [[Forbidden]]);
 *  - '''operation''' — coupling across nodes ([[SameInterval]]).
 *
 * Occupancy is not a constraint: it is the [[hub.operation.Interval]]s already
 * on operations (`actual`) plus intervals committed to the [[Plan]].
 *
 * {{{
 * Constraint.Capacity(gym, 2)
 * Constraint.Forbidden(room12, List(lunchBreak))
 * Constraint.SameInterval(List(assembly7a, assembly7b), Some(mondayEight))
 * }}}
 */
enum Constraint:
  /**
   * At most `max` operations may occupy `resource` at the same wall time.
   *
   * Absent capacity defaults to `1` (exclusive occupancy). Duplicate capacity for the
   * same id is rejected. `max` must be at least `1`.
   *
   * This is parallelism on one id (a gym hosting two PE lessons), not a set of
   * interchangeable rooms.
   */
  case Capacity(resource: ResourceId, max: Int)

  /**
   * `resource` cannot be used during half-open `windows`.
   *
   * Blocked wall-time (lunch, a closed room, after hours). Always a hard block, even when
   * [[Capacity]] allows overlap.
   */
  case Forbidden(resource: ResourceId, windows: List[HalfOpenWindow])

  /**
   * Listed operations share one wall-time interval.
   *
   * The scheduler picks one resource eligible for every member, one start/end
   * for the group, and books a single occupancy interval. Start is not earlier
   * than `minReady` when provided, and not earlier than each member's predecessor
   * ready floor.
   */
  case SameInterval(ops: List[OperationId], minReady: Option[LocalDateTime] = None)

end Constraint
