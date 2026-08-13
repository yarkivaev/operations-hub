package hub.resource.constraint

import java.time.LocalDateTime

/**
 * Half-open time interval `[start, end)` on the planning timeline.
 *
 * `start` is included, `end` is excluded. Two adjacent windows do not overlap at
 * the boundary: an interval ending exactly at 10:00 does not collide with a
 * blocked window starting at 10:00.
 *
 * Used by [[hub.schedule.Constraint.Forbidden]] and anywhere the planner needs a
 * resource-free time range. Overlap with `[spanStart, spanStart + duration)`
 * is tested as `spanStart < end && spanEnd > start`.
 *
 * {{{
 * HalfOpenWindow(
 *   LocalDateTime.of(2026, 6, 1, 12, 0),
 *   LocalDateTime.of(2026, 6, 1, 13, 0),
 * )
 * }}}
 *
 * @param start inclusive start of the window
 * @param end   exclusive end of the window
 */
final case class HalfOpenWindow(start: LocalDateTime, end: LocalDateTime)
