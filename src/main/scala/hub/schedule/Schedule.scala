package hub.schedule

import hub.operation.Operation
import hub.resource.Resource

import java.time.LocalDateTime

/**
 * Stateless scheduler: each [[plan]] call builds a disposable [[Plan]] from an
 * operation graph, constraints, and resources.
 *
 * Pass [[prior]] to keep feasible rows from a previous plan (warm-start).
 * Pass [[occupied]] to book foreign resource intervals without placing those ids.
 *
 * {{{
 * scheduler.plan(lessons, constraints, rooms, mondayEight)
 * scheduler.plan(lessons, constraints, rooms, mondayEight, lastPlan)
 * scheduler.plan(lessons, constraints, rooms, mondayEight, occupied = siblingPlan)
 * }}}
 */
trait Schedule[F[_]]:
  /**
   * Places remaining operations (those without a closed actual) onto resources.
   *
   * @param operations graph nodes (facts via `actual`, edges via `successor`)
   * @param constraints world and coupling rules for this run
   * @param resources   catalog identity (id and tags)
   * @param now         planning anchor / ready floor for roots
   * @param prior       previous plan whose feasible atomic rows are kept
   * @param occupied    foreign occupancy that blocks resources but is not placed
   */
  def plan(
      operations: List[Operation],
      constraints: List[Constraint],
      resources: List[Resource],
      now: LocalDateTime,
      prior: Plan = Plan.empty,
      occupied: Plan = Plan.empty,
  ): F[Plan]
