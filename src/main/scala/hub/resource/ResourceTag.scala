package hub.resource

/**
 * Label used by [[ResourceRequirement.AnyTagged]] to group catalog rows.
 *
 * Tags group interchangeable catalog ids (e.g. every chemistry room `"lab"`).
 * Parallelism on one id is [[hub.schedule.Constraint.Capacity]], not a tag.
 *
 * {{{
 * ResourceTag("lab")
 * ResourceTag("classroom")
 * }}}
 */
opaque type ResourceTag = String

object ResourceTag:
  /** Trims and wraps a raw tag string */
  def apply(raw: String): ResourceTag = raw.strip

  extension (tag: ResourceTag)
    /** Underlying tag text */
    def value: String = tag

end ResourceTag
