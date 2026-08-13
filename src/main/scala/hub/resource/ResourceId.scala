package hub.resource

import cats.Show
import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV

private[resource] object RefinedUnpackNe:
  def nePlain(r: String Refined NonEmpty): String =
    r.value

/**
 * Non-empty identifier of one [[Resource]] in the catalog.
 *
 * {{{
 * ResourceId.parse("room-12")
 * ResourceId.parse("teacher-anna").map(_.value)
 * }}}
 */
opaque type ResourceId = String Refined NonEmpty

object ResourceId:
  /** Wraps an already refined non-empty string */
  def fromRefined(r: String Refined NonEmpty): ResourceId = r

  /** Validates and trims a raw id string */
  def parse(raw: String): Either[String, ResourceId] =
    refineV[NonEmpty](raw.strip).left.map(m => s"ResourceId: $m").map(fromRefined)

  given Show[ResourceId] = Show.show(_.value)

  given Ordering[ResourceId] = Ordering.by(_.value)

  extension (o: ResourceId)
    /** Underlying non-empty string */
    def value: String = RefinedUnpackNe.nePlain(o)

end ResourceId
