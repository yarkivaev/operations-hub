package hub.schedule

import hub.operation.{Body, Operation}
import hub.resource.{Resource, ResourceId, ResourceRequirement, Resources}

/**
 * Resolves eligible resources for one atomic operation against the catalog.
 *
 * Turns [[hub.resource.ResourceRequirement]] into concrete [[Resource]] rows.
 * World rules ([[Constraint.Capacity]], [[Constraint.Forbidden]]) are applied
 * later by [[ResourceSlot]]. Composite operations have no resource need.
 *
 * {{{
 * val rooms = OperationEligibility.candidates(algebra, catalog)
 * }}}
 */
object OperationEligibility:
  /** Eligible catalog resources for an atomic `op`, sorted by id */
  def candidates(op: Operation, catalog: Resources): List[Resource] =
    op.body match
      case Body.Composite(_) => Nil
      case Body.Atom(kind) =>
        resolve(kind.requirement, catalog).map(catalog.get).sortBy(_.id.value)

  private def resolve(req: ResourceRequirement, catalog: Resources): List[ResourceId] =
    req match
      case ResourceRequirement.Unrestricted => Nil
      case ResourceRequirement.OneOf(ids)    => ids
      case ResourceRequirement.AnyTagged(tag) =>
        catalog.rows.filter(_.tags.contains(tag)).map(_.id)
      case ResourceRequirement.Count(tag, n) =>
        catalog.rows.filter(_.tags.contains(tag)).map(_.id).take(n)
      case ResourceRequirement.AllOf(specs) =>
        specs.flatMap(resolve(_, catalog)).distinct
      case ResourceRequirement.Optional(spec) =>
        resolve(spec, catalog)

end OperationEligibility
