package hub.planhttp

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import munit.FunSuite
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{Method, Request, Uri}

import java.net.ServerSocket
import scala.concurrent.duration.*

final class PlanHttpDemoServerSuite extends FunSuite:

  test("PlanHttpDemoServer serves health while running"):
    val port = ephemeralPort()
    val body =
      PlanHttpDemoServer
        .serve("127.0.0.1", port, "0.5.0")
        .use: _ =>
          EmberClientBuilder
            .default[IO]
            .build
            .use: client =>
              val request =
                Request[IO](
                  Method.GET,
                  Uri.unsafeFromString(s"http://127.0.0.1:$port/health"),
                )
              client.expect[String](request)
        .unsafeRunTimed(30.seconds)
        .getOrElse(sys.error("demo health request timed out"))
    assertThat("demo health body was not ok", body, is("ok"))

  private def ephemeralPort(): Int =
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()

end PlanHttpDemoServerSuite
