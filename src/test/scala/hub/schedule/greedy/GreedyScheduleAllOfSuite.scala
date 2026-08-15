package hub.schedule.greedy

import munit.FunSuite
import hub.support.OperationGraphMatchers.*
import hub.support.OperationGraphScenarios

final class GreedyScheduleAllOfSuite extends FunSuite:

  test("GreedySchedule shifts AllOf machine and person past person vacation"):
    assertThat(OperationGraphScenarios.allOfPersonVacationShiftsBoth(), allOfVacationBooksBothAfterForbidden)

  test("GreedySchedule does not overlap two AllOf ops on one person"):
    assertThat(OperationGraphScenarios.allOfPersonExclusiveAcrossTwoOps(), allOfPersonIntervalsDoNotOverlap)

end GreedyScheduleAllOfSuite
