package hub.schedule.greedy

import munit.FunSuite
import hub.support.OperationGraphMatchers.*
import hub.support.OperationGraphScenarios

final class GreedyScheduleWarmStartSuite extends FunSuite:

  test("GreedySchedule reuses feasible prior intervals on the same graph"):
    assertThat(OperationGraphScenarios.warmStartReusesFeasiblePrior(), warmReusesSameIntervals)

  test("GreedySchedule keeps history prior start after algebra becomes a fact"):
    assertThat(OperationGraphScenarios.warmStartKeepsHistoryAfterAlgebraFact(), warmKeepsHistoryStart)

  test("GreedySchedule keeps algebra and shifts history under a forbidden window"):
    assertThat(OperationGraphScenarios.warmStartShiftsHistoryUnderForbidden(), warmKeepsAlgebraShiftsHistory)

  test("GreedySchedule drops a prior row when the operation leaves the graph"):
    assertThat(OperationGraphScenarios.warmStartDropsRemovedNode(), warmDropsRemovedKeepsAlgebra)

  test("GreedySchedule places a fractions repeat and moves decimals after it"):
    assertThat(OperationGraphScenarios.warmStartPlacesRepeatBeforeKeptTail(), warmRepeatBeforeShiftedDecimals)

end GreedyScheduleWarmStartSuite
