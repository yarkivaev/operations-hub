package hub.schedule.greedy

import munit.FunSuite
import hub.support.OperationGraphMatchers.*
import hub.support.OperationGraphScenarios

final class GreedyScheduleGraphSuite extends FunSuite:

  test("GreedySchedule plans algebra then history for class 7a"):
    assertThat(OperationGraphScenarios.linearTwoSteps(), plansBothSteps)

  test("GreedySchedule starts history after algebra ends"):
    assertThat(OperationGraphScenarios.linearTwoSteps(), secondStartsAfterFirst)

  test("GreedySchedule schedules only the preferred elective after homeroom"):
    assertThat(OperationGraphScenarios.oneOfPicksPriorityBranch(), schedulesOnlyMainBranch)

  test("GreedySchedule starts chemistry and pe together after assembly"):
    assertThat(OperationGraphScenarios.allSuccessorsOverlap(), parallelShareParentReadyFloor)

  test("GreedySchedule starts history after a completed algebra lesson"):
    assertThat(OperationGraphScenarios.completedPrefixSkipsFact(), pendingStartsAfterFact)

  test("GreedySchedule places two assembly groups on one shared slot"):
    assertThat(OperationGraphScenarios.sameIntervalSharesSlot(), sharedSameInterval)

  test("GreedySchedule starts pe after the gym is closed"):
    assertThat(OperationGraphScenarios.respectsResourceDowntime(), startsAfterDowntime)

  test("GreedySchedule overlaps two pe lessons when the gym allows it"):
    assertThat(OperationGraphScenarios.capacityAllowsOverlap(), overlappingCapacityStartsTogether)

end GreedyScheduleGraphSuite
