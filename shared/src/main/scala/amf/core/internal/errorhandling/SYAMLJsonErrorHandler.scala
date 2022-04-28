package amf.core.internal.errorhandling

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.plugins.syntax.SYamlAMFParserErrorHandler
import amf.core.internal.validation.CoreValidations.SyamlWarning
import org.mulesoft.common.client.lexical.SourceLocation
import org.yaml.model.{DefaultJsonErrorHandler, ParseErrorHandler, SyamlException}

case class SYAMLJsonErrorHandler(eh: AMFErrorHandler) extends DefaultJsonErrorHandler {

  override val errorHandler: ParseErrorHandler = new SYamlAMFParserErrorHandler(eh)

  override protected def onIgnoredException(location: SourceLocation, e: SyamlException): Unit =
    eh.warning(SyamlWarning, "", e.getMessage, location)
}
