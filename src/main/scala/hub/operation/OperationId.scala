package hub.operation

import cats.Show
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV

/**
 * Stable operation key.
 *
 * The hub treats the id as an opaque non-empty string. Callers may compose
 * segments with `/`; the library does not interpret the encoding.
 *
 * {{{
 * OperationId.unsafe("7a/algebra/1")
 * OperationId.parse("7a/history/1")
 * }}}
 */
opaque type OperationId = String Refined NonEmpty

object OperationId:
  /** Path separator used when callers compose ids */
  val Sep: String = "/"

  /** Validates and trims a raw id string */
  def parse(raw: String): Either[String, OperationId] =
    refineV[NonEmpty](raw.strip).left.map(msg => s"invalid operation id: $msg, value=${raw.strip}")

  /** Parses or throws [[IllegalArgumentException]] */
  def unsafe(raw: String): OperationId =
    parse(raw).fold(e => throw IllegalArgumentException(e), identity)

  given Show[OperationId] = Show.show(_.text)

  given Ordering[OperationId] = Ordering.by(_.text)

  extension (id: OperationId)
    /** Underlying non-empty string */
    def text: String = id.value

    /**
     * Last `/`-separated segment parsed as int when possible.
     * Useful when callers encode a trailing ordinal in the id.
     */
    def templateNumeric: Int =
      val parts = id.text.split(Sep)
      parts.lastOption.flatMap(_.toIntOption).getOrElse(0)

end OperationId
