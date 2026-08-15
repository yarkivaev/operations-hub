package hub.operation

import hub.resource.ResourceId

import java.time.LocalDateTime

/**
 * Half-open wall-time span `[start, end)` with assigned catalog resource ids.
 *
 * [[resource]] is the primary id (first booked) for source-compatible call sites
 * and single-column exports. [[booked]] holds every id when an operation needs
 * more than one resource on the same interval ([[hub.resource.ResourceRequirement.AllOf]]).
 *
 * {{{
 * Interval(mondayEight, mondayEight.plusMinutes(45), Some(room12))
 * Interval(mondayEight, mondayEight.plusMinutes(45), Some(lathe), List(lathe, anna))
 * }}}
 *
 * @param start    inclusive start
 * @param end      exclusive end
 * @param resource primary catalog id, if any
 * @param booked   all booked ids when multi-resource; empty means [[resource]].toList
 */
final case class Interval(
    start: LocalDateTime,
    end: LocalDateTime,
    resource: Option[ResourceId] = None,
    booked: List[ResourceId] = Nil,
):

  /** Occupancy set used by the scheduler and [[hub.schedule.Plan.byResource]] */
  def resources: List[ResourceId] =
    if booked.nonEmpty then booked else resource.toList

end Interval
