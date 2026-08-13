package hub.schedule

import hub.resource.ResourceId

import java.time.{Duration, LocalDateTime}
import scala.annotation.tailrec

/**
 * Helpers for exclusive (capacity-1) placement against half-open downtime.
 *
 * Merges overlapping windows per resource and advances a proposed start so
 * `[start, start + duration)` does not intersect any window.
 */
object ResourceDowntimeWindows:

  /** Merges overlapping half-open windows per resource id */
  def merged(rows: List[(ResourceId, LocalDateTime, LocalDateTime)]): Map[ResourceId, List[(LocalDateTime, LocalDateTime)]] =
    rows.groupBy(_._1).map: (mid, xs) =>
      val sorted = xs.map((_, a, b) => (a, b)).sortBy(_._1)
      mid -> sorted.foldLeft(List.empty[(LocalDateTime, LocalDateTime)]):
        (acc, se) =>
          val (s, e) = se
          acc.lastOption match
            case Some((ps, pe)) if !s.isAfter(pe) =>
              acc.init :+ ((ps, if e.isAfter(pe) then e else pe))
            case _ =>
              acc :+ ((s, e))

  /**
   * Earliest start at or after `candidate` so `[result, result + duration)`
   * does not intersect any merged window for `resource`.
   */
  def slotStart(
      resource: ResourceId,
      candidate: LocalDateTime,
      duration: Duration,
      byResource: Map[ResourceId, List[(LocalDateTime, LocalDateTime)]]
  ): LocalDateTime =
    val d = if duration.isNegative || duration.isZero then Duration.ofNanos(1L) else duration
    @tailrec
    def loop(t: LocalDateTime): LocalDateTime =
      val hit = byResource
        .getOrElse(resource, Nil)
        .find: (a, b) =>
          t.isBefore(b) && t.plus(d).isAfter(a)
      hit match
        case Some((_, b)) => loop(b)
        case None         => t
    loop(candidate)

end ResourceDowntimeWindows
