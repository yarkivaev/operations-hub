package hub.resource

/**
 * Catalog of resources indexed by [[ResourceId]].
 *
 * Used by the scheduler when resolving [[ResourceRequirement]] against catalog
 * identity (id and tags). World rules live on [[hub.schedule.Constraint]].
 *
 * {{{
 * val catalog = Resources(List(Resource(room12), Resource(gym)))
 * val row = catalog.get(room12)
 * catalog.rows
 * }}}
 */
final class Resources private (catalogRows: List[Resource], byId: Map[ResourceId, Resource]):
  /** All catalog rows in insertion order */
  def rows: List[Resource] = catalogRows

  /**
   * Looks up a resource by id.
   * Missing ids yield [[Resource.stub]] so planning can still name the id.
   */
  def get(id: ResourceId): Resource =
    byId.getOrElse(id, Resource.stub(id))

object Resources:
  /** Indexes `rows` by id; later duplicates overwrite earlier ones in the map */
  def apply(rows: List[Resource]): Resources =
    new Resources(rows, rows.map(m => m.id -> m).toMap)

end Resources
