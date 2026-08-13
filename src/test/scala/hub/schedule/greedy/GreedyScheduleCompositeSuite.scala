package hub.schedule.greedy

import munit.FunSuite
import hub.support.OperationGraphMatchers.*
import hub.support.OperationGraphScenarios

final class GreedyScheduleCompositeSuite extends FunSuite:

  test("GreedySchedule envelopes monday from algebra through history without a room"):
    assertThat(OperationGraphScenarios.mondayCompositeDay(), mondayEnvelopeWithoutRoom)

  test("GreedySchedule starts tuesday after monday composite envelope"):
    assertThat(OperationGraphScenarios.mondayThenTuesday(), tuesdayStartsAfterMondayEnvelope)

  test("GreedySchedule envelopes nested week through monday lessons"):
    assertThat(OperationGraphScenarios.weekContainsMonday(), nestedWeekEnvelope)

  test("GreedySchedule skips parts when monday composite already has an actual"):
    assertThat(OperationGraphScenarios.compositeActualSkipsParts(), compositeActualLeavesPartsUnplanned)

  test("GreedySchedule includes algebra fact in monday envelope"):
    assertThat(OperationGraphScenarios.compositeEnvelopeIncludesPartFact(), envelopeUsesAlgebraFact)

  test("GreedySchedule rejects an empty composite parts list"):
    intercept[IllegalArgumentException](OperationGraphScenarios.emptyCompositeParts())

  test("GreedySchedule rejects a composite nesting cycle"):
    intercept[IllegalArgumentException](OperationGraphScenarios.compositeCycle())

  test("GreedySchedule rejects a part owned by two composites"):
    intercept[IllegalArgumentException](OperationGraphScenarios.sharedCompositePart())

  test("GreedySchedule rejects a part successor outside its composite"):
    intercept[IllegalArgumentException](OperationGraphScenarios.partSuccessorLeavesComposite())

  test("GreedySchedule rejects SameInterval on a composite"):
    intercept[IllegalArgumentException](OperationGraphScenarios.sameIntervalOnComposite())

end GreedyScheduleCompositeSuite
