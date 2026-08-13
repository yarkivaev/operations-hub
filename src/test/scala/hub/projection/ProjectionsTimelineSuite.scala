package hub.projection

import munit.FunSuite
import hub.projection.Projections
import hub.support.OperationGraphMatchers.assertThat
import hub.support.OperationGraphScenarios
import org.hamcrest.{Description, TypeSafeMatcher}

final class ProjectionsTimelineSuite extends FunSuite:

  test("Projections timeline includes planned intervals for algebra then history"):
    assertThat(
      OperationGraphScenarios.linearTwoSteps(),
      new TypeSafeMatcher[OperationGraphScenarios.LinearPlanView]:
        override def matchesSafely(item: OperationGraphScenarios.LinearPlanView): Boolean =
          val rows = Projections.timeline(item.operations, item.plan)
          rows.length == 2 && rows.forall(_.planned.nonEmpty)
        override def describeTo(d: Description): Unit =
          d.appendText("timeline with planned rows for algebra and history")
        override def describeMismatchSafely(item: OperationGraphScenarios.LinearPlanView, d: Description): Unit =
          d.appendText("timeline was ").appendValue(Projections.timeline(item.operations, item.plan)),
    )

end ProjectionsTimelineSuite
