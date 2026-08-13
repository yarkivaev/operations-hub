package hub.planexport

import hub.schedule.Plan

import java.io.{OutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets

/**
 * Renders a [[Plan]] as CSV of operation intervals.
 *
 * Columns: `operation,start,end,resource`. Presentation grids belong in the
 * caller; this module only serializes hub plan rows.
 *
 * {{{
 * PlanExport.write(timetable, outputStream)
 * }}}
 */
object PlanExport:
  /** Writes UTF-8 CSV for all planned intervals, ordered by start then id */
  def write(plan: Plan, out: OutputStream): Unit =
    val writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)
    try
      writer.write("operation,start,end,resource\n")
      plan.intervals.toList
        .sortBy((id, interval) => (interval.start, id.text))
        .foreach: (id, interval) =>
          val resource = interval.resource.map(_.value).getOrElse("")
          writer.write(s"${id.text},${interval.start},${interval.end},$resource\n")
      writer.flush()
    finally writer.close()

end PlanExport
