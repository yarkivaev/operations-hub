package hub.schedule

import hub.operation.{Body, Operation}
import hub.resource.{Resource, ResourceId, ResourceRequirement, Resources}

/**
 * Resolves eligible resources for one atomic operation against the catalog.
 *
 * [[candidates]] is the flat union of eligible ids (SameInterval intersection).
 * [[groups]] is the set of complete bookings: one resource per [[ResourceRequirement.AllOf]]
 * arm, as a Cartesian product of inner specs.
 *
 * {{{
 * val rooms = OperationEligibility.candidates(algebra, catalog)
 * val combos = OperationEligibility.groups(lesson, catalog)
 * }}}
 */
object OperationEligibility:

  /** Eligible catalog resources for an atomic `op`, sorted by id */
  def candidates(op: Operation, catalog: Resources): List[Resource] =
    op.body match
      case Body.Composite(_) => Nil
      case Body.Atom(kind) =>
        resolve(kind.requirement, catalog).map(catalog.get).sortBy(_.id.value)

  /**
   * Complete multi-resource bookings for an atomic `op`.
   *
   * Each inner list is one legal simultaneous assignment (arm order preserved).
   * Empty when the op needs no resource or no combo exists.
   */
  def groups(op: Operation, catalog: Resources): List[List[Resource]] =
    op.body match
      case Body.Composite(_) => Nil
      case Body.Atom(kind) =>
        groupIds(kind.requirement, catalog)
          .map(_.map(catalog.get))
          .sortBy(_.map(_.id.value).mkString("|"))

  private def resolve(req: ResourceRequirement, catalog: Resources): List[ResourceId] =
    groupIds(req, catalog).flatten.distinct

  private def groupIds(req: ResourceRequirement, catalog: Resources): List[List[ResourceId]] =
    req match
      case ResourceRequirement.Unrestricted => Nil
      case ResourceRequirement.OneOf(ids)    => ids.map(List(_))
      case ResourceRequirement.AnyTagged(tag) =>
        catalog.rows.filter(_.tags.contains(tag)).map(row => List(row.id))
      case ResourceRequirement.Count(tag, n) =>
        val ids = catalog.rows.filter(_.tags.contains(tag)).map(_.id).take(n)
        if ids.isEmpty then Nil else List(ids)
      case ResourceRequirement.AllOf(specs) =>
        val arms = specs.map(groupIds(_, catalog))
        if arms.exists(_.isEmpty) then Nil else product(arms)
      case ResourceRequirement.Optional(spec) =>
        groupIds(spec, catalog)

  private def product(arms: List[List[List[ResourceId]]]): List[List[ResourceId]] =
    arms.foldLeft(List(List.empty[ResourceId])): (acc, arm) =>
      for
        prefix <- acc
        pick <- arm
      yield prefix ++ pick

end OperationEligibility
