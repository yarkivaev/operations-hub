package hub.planhttp

import cats.Id
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.{Host, Port}
import hub.operation.*
import hub.resource.*
import hub.schedule.Constraint
import hub.schedule.greedy.GreedySchedule
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

import java.nio.file.{Files, Paths}
import java.time.{Duration, LocalDateTime}

/**
 * Minimal school-timetable demo over [[PlanHttpRead]].
 *
 * Plans one Monday composite (algebra then history) and serves immutable
 * read routes. Start with `planHttp/run`.
 *
 * {{{
 * PlanHttpDemoServer.main(Array("--host", "127.0.0.1", "--port", "8080"))
 * }}}
 */
object PlanHttpDemoServer extends IOApp:

  /** Runs the demo server until interrupted */
  def run(args: List[String]): IO[ExitCode] =
    val host = argValue(args, "--host").getOrElse("127.0.0.1")
    val port = argValue(args, "--port").map(_.toInt).getOrElse(8080)
    serve(host, port, readVersion).useForever.as(ExitCode.Success)

  /**
   * Builds an http4s server with a frozen school-day plan.
   *
   * @param host    bind host
   * @param port    bind port
   * @param version plan-http version advertised by `/api/v1/meta`
   */
  def serve(host: String, port: Int, version: String): Resource[IO, Server] =
    val view = schoolDayReadView()
    val routes = PlanHttpRead.routes(IO.pure(view), version)
    EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString(host).getOrElse(sys.error(s"invalid host $host")))
      .withPort(Port.fromInt(port).getOrElse(sys.error(s"invalid port $port")))
      .withHttpApp(routes.orNotFound)
      .build

  /** Frozen Monday composite plan used by the demo and its suite. */
  def schoolDayReadView(): PlanHttpReadView =
    val monday = OperationId.unsafe("7a/monday/1")
    val algebra = OperationId.unsafe("7a/algebra/1")
    val history = OperationId.unsafe("7a/history/1")
    val roomId = ResourceId.parse("room-12").fold(e => throw IllegalArgumentException(e), identity)
    val room = hub.resource.Resource(roomId, Set.empty[ResourceTag])
    val algebraKind =
      OperationKind("algebra", Duration.ofMinutes(45), ResourceRequirement.OneOf(List(roomId)))
    val historyKind =
      OperationKind("history", Duration.ofMinutes(45), ResourceRequirement.OneOf(List(roomId)))
    val operations =
      List(
        Operation(monday, Body.Composite(List(algebra, history)), Successor.Done),
        Operation(algebra, algebraKind, Successor.Then(history)),
        Operation(history, historyKind, Successor.Done),
      )
    val now = LocalDateTime.of(2026, 6, 1, 8, 0)
    val plan = GreedySchedule.live[Id].plan(operations, List.empty[Constraint], List(room), now)
    PlanHttpReadView(operations, Nil, List(room), plan)

  private def readVersion: String =
    List(Paths.get("VERSION"), Paths.get("../VERSION"))
      .find(Files.exists(_))
      .map(path => Files.readString(path).trim)
      .filter(_.nonEmpty)
      .getOrElse(sys.error("VERSION file missing"))

  private def argValue(args: List[String], flag: String): Option[String] =
    args.zip(args.drop(1)).collectFirst { case (`flag`, value) => value }

end PlanHttpDemoServer
