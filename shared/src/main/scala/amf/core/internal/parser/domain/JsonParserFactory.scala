package amf.core.internal.parser.domain

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.errorhandling
import org.mulesoft.common.client.lexical.Position
import org.yaml.parser.JsonParser
object JsonParserFactory {

  def fromChars(s: CharSequence)(implicit errorhandler: AMFErrorHandler): JsonParser =
    JsonParser(s)(errorhandling.SYAMLJsonErrorHandler(errorhandler))

  def fromCharsWithSource(s: CharSequence, sourceName: String, positionOffset: Position = Position.ZERO)(
      implicit errorHandler: AMFErrorHandler): JsonParser =
    JsonParser.withSource(s, sourceName, positionOffset)(errorhandling.SYAMLJsonErrorHandler(errorHandler))
}
