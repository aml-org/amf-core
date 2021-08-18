package amf.core.internal.parser.domain

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.errorhandling
import amf.core.internal.errorhandling.SYAMLJsonErrorHandler
import org.yaml.parser.JsonParser
import org.mulesoft.lexer.{Position => SyamlPosition}
object JsonParserFactory {

  def fromChars(s: CharSequence)(implicit errorhandler: AMFErrorHandler): JsonParser =
    JsonParser(s)(errorhandling.SYAMLJsonErrorHandler(errorhandler))

  def fromCharsWithSource(s: CharSequence, sourceName: String, positionOffset: SyamlPosition = SyamlPosition.Zero)(
      implicit errorHandler: AMFErrorHandler): JsonParser =
    JsonParser.withSource(s, sourceName, positionOffset)(errorhandling.SYAMLJsonErrorHandler(errorHandler))
}
