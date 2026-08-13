package hub.resource

import cats.Show

/**
 * Catalog identity of one schedulable capacity unit.
 *
 * A resource is only ''what it is'': a stable [[id]] and optional [[tags]] for
 * eligibility ([[ResourceRequirement.AnyTagged]]). How many operations may overlap,
 * when it is unavailable, and what is already booked are not part of the
 * resource — they are [[hub.schedule.Constraint]] and operation [[hub.operation.Interval]]s.
 *
 * {{{
 * Resource(room12)
 * Resource(labChem, Set(ResourceTag("lab")))
 * Resource.stub(gym)
 * }}}
 *
 * @param id   stable resource key
 * @param tags labels used by tag-based eligibility
 */
final case class Resource(
    id: ResourceId,
    tags: Set[ResourceTag] = Set.empty,
)

object Resource:
  given Show[Resource] = Show.show: row =>
    s"Resource(id=${row.id.value})"

  /** Catalog row with no tags */
  def stub(id: ResourceId): Resource =
    Resource(id, Set.empty)

end Resource
