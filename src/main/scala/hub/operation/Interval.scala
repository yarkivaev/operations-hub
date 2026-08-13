package hub.operation

import hub.resource.ResourceId

import java.time.LocalDateTime

/**
 * Half-open wall-time span `[start, end)` with optional assigned resource id.
 *
 * Shared shape for execution facts ([[Operation.actual]]) and plan rows
 * ([[hub.schedule.Plan]]). The assignment is a catalog key, not a snapshot of
 * resource constraints.
 *
 * Overlap uses the half-open law: adjacent bounds do not collide.
 *
 * {{{
 * Interval(mondayEight, mondayEight.plusMinutes(45), Some(room12))
 * Interval(mondayEight, mondayEight.plusMinutes(45), None)
 * }}}
 *
 * @param start    inclusive start
 * @param end      exclusive end
 * @param resource assigned catalog id, if any
 */
final case class Interval(
    start: LocalDateTime,
    end: LocalDateTime,
    resource: Option[ResourceId] = None,
)
