package hub.planhttp

import cats.effect.IO
import hub.operation.Operation
import hub.projection.Projections
import hub.resource.Resource
import hub.schedule.{Constraint, Plan}
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*

/**
 * Immutable snapshot passed to read routes on each request.
 *
 * Callers rebuild a fresh view after writes; the hub holds no session state.
 *
 * @param operations graph nodes
 * @param constraints cross-node rules used for the plan
 * @param resources   catalog used for the plan
 * @param plan        disposable schedule output
 */
final case class PlanHttpReadView(
    operations: List[Operation],
    constraints: List[Constraint],
    resources: List[Resource],
    plan: Plan,
)

/**
 * Immutable read HTTP over an operation graph and [[Plan]].
 *
 * Endpoints: `GET /health`, `GET /api/v1/meta`, `GET /api/v1/plan`,
 * `GET /api/v1/timeline`.
 *
 * {{{
 * PlanHttpRead.routes(IO.pure(view), "0.5.0")
 * }}}
 */
object PlanHttpRead:
  /**
   * Builds http4s routes that load a fresh [[PlanHttpReadView]] per request.
   *
   * @param load    effect that yields the current read snapshot
   * @param version plan-http version for the `/api/v1/meta` handshake
   */
  def routes(load: IO[PlanHttpReadView], version: String): HttpRoutes[IO] =
    HttpRoutes.of[IO]:
      case GET -> Root / "health" =>
        Ok("ok")
      case GET -> Root / "api" / "v1" / "meta" =>
        Ok(s"""{"api":"v1","planHttp":"$version","tag":"v$version"}""")
      case GET -> Root / "api" / "v1" / "plan" =>
        load.flatMap: view =>
          val body =
            view.plan.intervals.toList
              .sortBy((id, interval) => (interval.start, id.text))
              .map: (id, interval) =>
                s""""${id.text}":{"start":"${interval.start}","end":"${interval.end}"}"""
              .mkString("{", ",", "}")
          Ok(body)
      case GET -> Root / "api" / "v1" / "timeline" =>
        load.flatMap: view =>
          val rows = Projections.timeline(view.operations, view.plan)
          val body =
            rows
              .map: row =>
                s""""${row.operation.id.text}""""
              .mkString("[", ",", "]")
          Ok(body)

end PlanHttpRead
