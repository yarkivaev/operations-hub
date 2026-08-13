package hub.planexport

import munit.FunSuite
import hub.operation.{Interval, OperationId}
import hub.schedule.Plan
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*

import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

final class PlanExportSuite extends FunSuite:

  test("PlanExport writes lesson interval rows as csv"):
    val id = OperationId.unsafe("7a/algebra/1")
    val start = LocalDateTime.of(2026, 6, 1, 8, 0)
    val end = start.plusMinutes(45)
    val plan = Plan(Map(id -> Interval(start, end, hub.resource.ResourceId.parse("room-12").toOption)))
    val bytes = new ByteArrayOutputStream()
    PlanExport.write(plan, bytes)
    assertThat(
      "csv did not contain planned operation id",
      bytes.toString(java.nio.charset.StandardCharsets.UTF_8),
      containsString("7a/algebra/1"),
    )

end PlanExportSuite
