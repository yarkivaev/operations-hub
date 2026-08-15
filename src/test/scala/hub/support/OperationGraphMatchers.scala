package hub.support

import hub.operation.OperationId
import hub.schedule.Plan
import org.hamcrest.{Description, Matcher, TypeSafeMatcher}
import org.hamcrest.MatcherAssert.assertThat as hamAssertThat

import java.time.LocalDateTime

/**
 * Hamcrest matchers for operation-graph plan tests.
 */
object OperationGraphMatchers:

  def assertThat[A](actual: A, matcher: Matcher[? >: A]): Unit =
    hamAssertThat(actual, matcher)

  def plansBothSteps: Matcher[OperationGraphScenarios.LinearPlanView] =
    new TypeSafeMatcher[OperationGraphScenarios.LinearPlanView]:
      override def matchesSafely(item: OperationGraphScenarios.LinearPlanView): Boolean =
        item.plan.at(item.firstId).nonEmpty && item.plan.at(item.secondId).nonEmpty
      override def describeTo(d: Description): Unit =
        d.appendText("plan containing both algebra and history")
      override def describeMismatchSafely(item: OperationGraphScenarios.LinearPlanView, d: Description): Unit =
        d.appendText("intervals were ").appendValue(item.plan.intervals.keySet)

  def secondStartsAfterFirst: Matcher[OperationGraphScenarios.LinearPlanView] =
    new TypeSafeMatcher[OperationGraphScenarios.LinearPlanView]:
      override def matchesSafely(item: OperationGraphScenarios.LinearPlanView): Boolean =
        (for
          first <- item.plan.at(item.firstId)
          second <- item.plan.at(item.secondId)
        yield !second.start.isBefore(first.end)).getOrElse(false)
      override def describeTo(d: Description): Unit =
        d.appendText("history starting at or after algebra end")
      override def describeMismatchSafely(item: OperationGraphScenarios.LinearPlanView, d: Description): Unit =
        d.appendText("plan was ").appendValue(item.plan.intervals)

  def schedulesOnlyMainBranch: Matcher[OperationGraphScenarios.ForkPlanView] =
    new TypeSafeMatcher[OperationGraphScenarios.ForkPlanView]:
      override def matchesSafely(item: OperationGraphScenarios.ForkPlanView): Boolean =
        item.plan.at(item.mainId).nonEmpty && item.plan.at(item.altId).isEmpty
      override def describeTo(d: Description): Unit =
        d.appendText("plan scheduling only the preferred elective")
      override def describeMismatchSafely(item: OperationGraphScenarios.ForkPlanView, d: Description): Unit =
        d.appendText("intervals were ").appendValue(item.plan.intervals.keySet)

  def parallelShareParentReadyFloor: Matcher[OperationGraphScenarios.ParallelPlanView] =
    new TypeSafeMatcher[OperationGraphScenarios.ParallelPlanView]:
      override def matchesSafely(item: OperationGraphScenarios.ParallelPlanView): Boolean =
        (for
          left <- item.plan.at(item.leftId)
          right <- item.plan.at(item.rightId)
        yield !left.start.isBefore(item.parentEnd) && !right.start.isBefore(item.parentEnd) &&
          left.start == right.start).getOrElse(false)
      override def describeTo(d: Description): Unit =
        d.appendText("chemistry and pe starting together after assembly")
      override def describeMismatchSafely(item: OperationGraphScenarios.ParallelPlanView, d: Description): Unit =
        d.appendText("left=").appendValue(item.plan.at(item.leftId)).appendText(" right=").appendValue(item.plan.at(item.rightId))

  def pendingStartsAfterFact: Matcher[OperationGraphScenarios.PrefixPlanView] =
    new TypeSafeMatcher[OperationGraphScenarios.PrefixPlanView]:
      override def matchesSafely(item: OperationGraphScenarios.PrefixPlanView): Boolean =
        item.plan.at(item.pendingId).exists(interval => !interval.start.isBefore(item.expectedStart))
      override def describeTo(d: Description): Unit =
        d.appendText("history starting after completed algebra")
      override def describeMismatchSafely(item: OperationGraphScenarios.PrefixPlanView, d: Description): Unit =
        d.appendText("pending was ").appendValue(item.plan.at(item.pendingId))

  def sharedSameInterval: Matcher[OperationGraphScenarios.SameIntervalView] =
    new TypeSafeMatcher[OperationGraphScenarios.SameIntervalView]:
      override def matchesSafely(item: OperationGraphScenarios.SameIntervalView): Boolean =
        (for
          left <- item.plan.at(item.leftId)
          right <- item.plan.at(item.rightId)
        yield left.start == right.start && left.end == right.end).getOrElse(false)
      override def describeTo(d: Description): Unit =
        d.appendText("assembly groups sharing one wall interval")
      override def describeMismatchSafely(item: OperationGraphScenarios.SameIntervalView, d: Description): Unit =
        d.appendText("left=").appendValue(item.plan.at(item.leftId)).appendText(" right=").appendValue(item.plan.at(item.rightId))

  def startsAfterDowntime: Matcher[OperationGraphScenarios.DowntimePlanView] =
    new TypeSafeMatcher[OperationGraphScenarios.DowntimePlanView]:
      override def matchesSafely(item: OperationGraphScenarios.DowntimePlanView): Boolean =
        item.plan.at(item.opId).exists(interval => !interval.start.isBefore(item.downtimeEnd))
      override def describeTo(d: Description): Unit =
        d.appendText("pe starting after the gym is closed")
      override def describeMismatchSafely(item: OperationGraphScenarios.DowntimePlanView, d: Description): Unit =
        d.appendText("interval was ").appendValue(item.plan.at(item.opId))

  def overlappingCapacityStartsTogether: Matcher[OperationGraphScenarios.CapacityPlanView] =
    new TypeSafeMatcher[OperationGraphScenarios.CapacityPlanView]:
      override def matchesSafely(item: OperationGraphScenarios.CapacityPlanView): Boolean =
        (for
          left <- item.plan.at(item.leftId)
          right <- item.plan.at(item.rightId)
        yield left.start == right.start).getOrElse(false)
      override def describeTo(d: Description): Unit =
        d.appendText("two pe lessons starting at the same time")
      override def describeMismatchSafely(item: OperationGraphScenarios.CapacityPlanView, d: Description): Unit =
        d.appendText("left=").appendValue(item.plan.at(item.leftId)).appendText(" right=").appendValue(item.plan.at(item.rightId))

  def mondayEnvelopeWithoutRoom: Matcher[OperationGraphScenarios.CompositeDayView] =
    new TypeSafeMatcher[OperationGraphScenarios.CompositeDayView]:
      override def matchesSafely(item: OperationGraphScenarios.CompositeDayView): Boolean =
        (for
          monday <- item.plan.at(item.mondayId)
          algebra <- item.plan.at(item.algebraId)
          history <- item.plan.at(item.historyId)
        yield monday.resource.isEmpty && monday.start == algebra.start && monday.end == history.end).getOrElse(false)
      override def describeTo(d: Description): Unit =
        d.appendText("monday envelope from algebra start to history end without a room")
      override def describeMismatchSafely(item: OperationGraphScenarios.CompositeDayView, d: Description): Unit =
        d.appendText("plan was ").appendValue(item.plan.intervals)

  def tuesdayStartsAfterMondayEnvelope: Matcher[OperationGraphScenarios.CompositeThenView] =
    new TypeSafeMatcher[OperationGraphScenarios.CompositeThenView]:
      override def matchesSafely(item: OperationGraphScenarios.CompositeThenView): Boolean =
        (for
          monday <- item.plan.at(item.mondayId)
          tuesday <- item.plan.at(item.tuesdayId)
        yield !tuesday.start.isBefore(monday.end)).getOrElse(false)
      override def describeTo(d: Description): Unit =
        d.appendText("tuesday starting at or after monday envelope end")
      override def describeMismatchSafely(item: OperationGraphScenarios.CompositeThenView, d: Description): Unit =
        d.appendText("plan was ").appendValue(item.plan.intervals)

  def nestedWeekEnvelope: Matcher[OperationGraphScenarios.NestedCompositeView] =
    new TypeSafeMatcher[OperationGraphScenarios.NestedCompositeView]:
      override def matchesSafely(item: OperationGraphScenarios.NestedCompositeView): Boolean =
        (for
          week <- item.plan.at(item.weekId)
          monday <- item.plan.at(item.mondayId)
          algebra <- item.plan.at(item.algebraId)
          history <- item.plan.at(item.historyId)
        yield week.resource.isEmpty && monday.resource.isEmpty &&
          week.start == algebra.start && week.end == history.end &&
          monday.start == algebra.start && monday.end == history.end).getOrElse(false)
      override def describeTo(d: Description): Unit =
        d.appendText("week and monday envelopes covering algebra through history")
      override def describeMismatchSafely(item: OperationGraphScenarios.NestedCompositeView, d: Description): Unit =
        d.appendText("plan was ").appendValue(item.plan.intervals)

  def compositeActualLeavesPartsUnplanned: Matcher[OperationGraphScenarios.CompositeActualView] =
    new TypeSafeMatcher[OperationGraphScenarios.CompositeActualView]:
      override def matchesSafely(item: OperationGraphScenarios.CompositeActualView): Boolean =
        item.plan.at(item.mondayId).isEmpty &&
          item.plan.at(item.algebraId).isEmpty &&
          item.plan.at(item.historyId).isEmpty
      override def describeTo(d: Description): Unit =
        d.appendText("no plan rows when monday already has an actual")
      override def describeMismatchSafely(item: OperationGraphScenarios.CompositeActualView, d: Description): Unit =
        d.appendText("intervals were ").appendValue(item.plan.intervals.keySet)

  def envelopeUsesAlgebraFact: Matcher[OperationGraphScenarios.CompositePartFactView] =
    new TypeSafeMatcher[OperationGraphScenarios.CompositePartFactView]:
      override def matchesSafely(item: OperationGraphScenarios.CompositePartFactView): Boolean =
        (for
          monday <- item.plan.at(item.mondayId)
          history <- item.plan.at(item.historyId)
        yield monday.resource.isEmpty &&
          monday.start == LocalDateTime.of(2026, 6, 1, 8, 0) &&
          monday.end == history.end).getOrElse(false)
      override def describeTo(d: Description): Unit =
        d.appendText("monday envelope starting at algebra fact and ending at history")
      override def describeMismatchSafely(item: OperationGraphScenarios.CompositePartFactView, d: Description): Unit =
        d.appendText("plan was ").appendValue(item.plan.intervals)

  def warmReusesSameIntervals: Matcher[OperationGraphScenarios.WarmReuseView] =
    new TypeSafeMatcher[OperationGraphScenarios.WarmReuseView]:
      override def matchesSafely(item: OperationGraphScenarios.WarmReuseView): Boolean =
        item.first.at(item.algebraId) == item.second.at(item.algebraId) &&
          item.first.at(item.historyId) == item.second.at(item.historyId)
      override def describeTo(d: Description): Unit =
        d.appendText("second plan reusing the same algebra and history intervals")
      override def describeMismatchSafely(item: OperationGraphScenarios.WarmReuseView, d: Description): Unit =
        d.appendText("first=").appendValue(item.first.intervals).appendText(" second=").appendValue(item.second.intervals)

  def warmKeepsHistoryStart: Matcher[OperationGraphScenarios.WarmFactView] =
    new TypeSafeMatcher[OperationGraphScenarios.WarmFactView]:
      override def matchesSafely(item: OperationGraphScenarios.WarmFactView): Boolean =
        item.plan.at(item.historyId).exists(_.start == item.priorHistoryStart)
      override def describeTo(d: Description): Unit =
        d.appendText("history keeping its prior start after algebra fact")
      override def describeMismatchSafely(item: OperationGraphScenarios.WarmFactView, d: Description): Unit =
        d.appendText("history was ").appendValue(item.plan.at(item.historyId))

  def warmKeepsAlgebraShiftsHistory: Matcher[OperationGraphScenarios.WarmForbiddenView] =
    new TypeSafeMatcher[OperationGraphScenarios.WarmForbiddenView]:
      override def matchesSafely(item: OperationGraphScenarios.WarmForbiddenView): Boolean =
        (for
          algebra <- item.plan.at(item.algebraId)
          history <- item.plan.at(item.historyId)
        yield algebra.start == item.priorAlgebraStart && history.start.isAfter(item.priorHistoryStart)).getOrElse(false)
      override def describeTo(d: Description): Unit =
        d.appendText("algebra kept and history shifted after a forbidden window")
      override def describeMismatchSafely(item: OperationGraphScenarios.WarmForbiddenView, d: Description): Unit =
        d.appendText("plan was ").appendValue(item.plan.intervals)

  def warmDropsRemovedKeepsAlgebra: Matcher[OperationGraphScenarios.WarmRemovedView] =
    new TypeSafeMatcher[OperationGraphScenarios.WarmRemovedView]:
      override def matchesSafely(item: OperationGraphScenarios.WarmRemovedView): Boolean =
        item.plan.at(item.removedId).isEmpty && item.plan.at(item.keptId).nonEmpty
      override def describeTo(d: Description): Unit =
        d.appendText("removed history absent and algebra still planned")
      override def describeMismatchSafely(item: OperationGraphScenarios.WarmRemovedView, d: Description): Unit =
        d.appendText("intervals were ").appendValue(item.plan.intervals.keySet)

  def warmRepeatBeforeShiftedDecimals: Matcher[OperationGraphScenarios.WarmRepeatView] =
    new TypeSafeMatcher[OperationGraphScenarios.WarmRepeatView]:
      override def matchesSafely(item: OperationGraphScenarios.WarmRepeatView): Boolean =
        (for
          repeat <- item.plan.at(item.repeatId)
          decimals <- item.plan.at(item.decimalsId)
        yield !decimals.start.isBefore(repeat.end) && decimals.start.isAfter(item.priorDecimalsStart)).getOrElse(false)
      override def describeTo(d: Description): Unit =
        d.appendText("fractions repeat placed and decimals starting after both prior start and repeat")
      override def describeMismatchSafely(item: OperationGraphScenarios.WarmRepeatView, d: Description): Unit =
        d.appendText("plan was ").appendValue(item.plan.intervals)

  def occupiedSecondStartsAtFirstEnd: Matcher[OperationGraphScenarios.OccupiedDeferView] =
    new TypeSafeMatcher[OperationGraphScenarios.OccupiedDeferView]:
      override def matchesSafely(item: OperationGraphScenarios.OccupiedDeferView): Boolean =
        !item.secondStart.isBefore(item.firstEnd)
      override def describeTo(d: Description): Unit =
        d.appendText("second graph starting at or after occupied first end")
      override def describeMismatchSafely(item: OperationGraphScenarios.OccupiedDeferView, d: Description): Unit =
        d.appendText("firstEnd=").appendValue(item.firstEnd).appendText(" secondStart=").appendValue(item.secondStart)

end OperationGraphMatchers
