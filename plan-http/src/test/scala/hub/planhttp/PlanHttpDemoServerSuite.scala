package hub.planhttp

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import munit.FunSuite
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.http4s.{Method, Request, Uri}

final class PlanHttpDemoServerSuite extends FunSuite:

  test("PlanHttpDemoServer school day view serves health through routes"):
    val view = PlanHttpDemoServer.schoolDayReadView()
    val routes = PlanHttpRead.routes(IO.pure(view), "0.5.0")
    val response =
      routes
        .run(Request[IO](Method.GET, Uri.unsafeFromString("/health")))
        .value
        .unsafeRunSync()
    val body = response.get.as[String].unsafeRunSync()
    assertThat("demo health body was not ok", body, is("ok"))

end PlanHttpDemoServerSuite
