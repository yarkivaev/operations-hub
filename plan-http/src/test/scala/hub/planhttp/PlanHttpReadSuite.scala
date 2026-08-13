package hub.planhttp

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import munit.FunSuite
import hub.schedule.Plan
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.http4s.{Method, Request, Uri}

final class PlanHttpReadSuite extends FunSuite:

  private val emptyView =
    PlanHttpReadView(
      operations = Nil,
      constraints = Nil,
      resources = Nil,
      plan = Plan.empty,
    )

  test("PlanHttpRead health endpoint returns ok"):
    val routes = PlanHttpRead.routes(IO.pure(emptyView), "0.5.0")
    val response =
      routes
        .run(Request[IO](Method.GET, Uri.unsafeFromString("/health")))
        .value
        .unsafeRunSync()
    assertThat("health route was missing", response.isDefined, is(true))

  test("PlanHttpRead meta endpoint returns version handshake"):
    val routes = PlanHttpRead.routes(IO.pure(emptyView), "0.5.0")
    val response =
      routes
        .run(Request[IO](Method.GET, Uri.unsafeFromString("/api/v1/meta")))
        .value
        .unsafeRunSync()
    val body = response.get.as[String].unsafeRunSync()
    assertThat(
      "meta body did not advertise plan-http version",
      body,
      containsString(""""planHttp":"0.5.0""""),
    )

end PlanHttpReadSuite
