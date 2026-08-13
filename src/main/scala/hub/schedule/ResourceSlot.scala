package hub.schedule

import hub.resource.ResourceId
import hub.resource.constraint.HalfOpenWindow

import java.time.{Duration, LocalDateTime}

/**
 * Finds the earliest feasible start on one resource.
 *
 * Combines occupancy, [[Constraint.Forbidden]] windows, and [[Constraint.Capacity]]:
 *  - `capacity <= 1` merges occupancy with forbidden time and advances past any hit;
 *  - `capacity > 1` treats forbidden windows as a hard block and allows up to
 *    `capacity` overlapping intervals.
 *
 * {{{
 * ResourceSlot.start(gym, candidate, duration, occupied, lunchBreak, 2)
 * }}}
 */
object ResourceSlot:
  /**
   * Earliest start at or after `candidate` for `[start, start + duration)`.
   *
   * @param resource  resource id (used when merging for exclusive capacity)
   * @param candidate lower bound for the search
   * @param duration  operation length
   * @param occupied  already booked intervals on this resource
   * @param forbidden blocked windows from [[Constraint.Forbidden]]
   * @param capacity  max overlapping intervals from [[Constraint.Capacity]], else 1
   */
  def start(
      resource: ResourceId,
      candidate: LocalDateTime,
      duration: Duration,
      occupied: List[(LocalDateTime, LocalDateTime)],
      forbidden: List[HalfOpenWindow],
      capacity: Int,
  ): LocalDateTime =
    val blocked = forbidden.map(w => (w.start, w.end))
    if capacity <= 1 then
      ResourceDowntimeWindows.slotStart(
        resource,
        candidate,
        duration,
        Map(resource -> (occupied ++ blocked)),
      )
    else
      val cap = capacity.max(1)
      var slot = candidate
      var guard = 0
      while guard < 1000 && !fits(slot, duration, occupied, blocked, cap) do
        slot = nextSlot(slot, duration, occupied, blocked)
        guard += 1
      slot

  private def fits(
      start: LocalDateTime,
      duration: Duration,
      occupied: List[(LocalDateTime, LocalDateTime)],
      forbidden: List[(LocalDateTime, LocalDateTime)],
      capacity: Int,
  ): Boolean =
    val end = start.plus(duration)
    if forbidden.exists(blocks(start, end)) then false
    else occupied.count(blocks(start, end)) < capacity

  private def blocks(start: LocalDateTime, end: LocalDateTime)(window: (LocalDateTime, LocalDateTime)): Boolean =
    val (from, until) = window
    start.isBefore(until) && end.isAfter(from)

  private def nextSlot(
      start: LocalDateTime,
      duration: Duration,
      occupied: List[(LocalDateTime, LocalDateTime)],
      forbidden: List[(LocalDateTime, LocalDateTime)],
  ): LocalDateTime =
    val end = start.plus(duration)
    val ends =
      occupied.filter(blocks(start, end)).map(_._2) ++
        forbidden.filter(blocks(start, end)).map(_._2)
    ends.sortBy(_.toString).headOption.getOrElse(start.plusSeconds(1))

end ResourceSlot
