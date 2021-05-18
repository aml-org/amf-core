package amf.core.parser

import amf.core.errorhandling.AMFErrorHandler
import amf.core.parser.errorhandler.JsonErrorHandler
import org.mulesoft.lexer.{Position => SyamlPosition}
import org.yaml.parser.JsonParser

object JsonParserFactory {

  def fromChars(s: CharSequence)(implicit errorhandler: AMFErrorHandler): JsonParser =
    JsonParser(s)(JsonErrorHandler(errorhandler))

  def fromCharsWithSource(s: CharSequence, sourceName: String, positionOffset: SyamlPosition = SyamlPosition.Zero)(
      implicit errorHandler: AMFErrorHandler): JsonParser =
    JsonParser.withSource(s, sourceName, positionOffset)(JsonErrorHandler(errorHandler))
}
