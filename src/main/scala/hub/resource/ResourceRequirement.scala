package hub.resource

/**
 * Declarative need for renewable capacity on one operation.
 *
 * Lives on [[hub.operation.OperationKind.requirement]] and describes which
 * catalog [[Resource]] rows may host the lesson. The scheduler resolves this
 * against the catalog, then applies [[hub.schedule.Constraint]] (capacity,
 * forbidden windows) when placing an interval.
 *
 * {{{
 * ResourceRequirement.OneOf(List(room12, room14, room15))
 * ResourceRequirement.AnyTagged(ResourceTag("lab"))
 * ResourceRequirement.AllOf(
 *   List(
 *     ResourceRequirement.OneOf(List(room12)),
 *     ResourceRequirement.OneOf(List(teacherAnna)),
 *   )
 * )
 * ResourceRequirement.Count(ResourceTag("classroom"), 2)
 * ResourceRequirement.Optional(ResourceRequirement.OneOf(List(projector)))
 * ResourceRequirement.Unrestricted
 * }}}
 */
enum ResourceRequirement:
  /** Pick exactly one resource from an explicit allow-list */
  case OneOf(ids: List[ResourceId])

  /** Pick one resource from every catalog row that carries `tag` */
  case AnyTagged(tag: ResourceTag)

  /**
   * Pick one resource per inner spec; all picks share the same lesson interval.
   */
  case AllOf(specs: List[ResourceRequirement])

  /** Pick up to `n` catalog rows with `tag` */
  case Count(tag: ResourceTag, n: Int)

  /** Same resolution rules as the wrapped spec when present */
  case Optional(spec: ResourceRequirement)

  /** Lesson needs no resource assignment */
  case Unrestricted

end ResourceRequirement
