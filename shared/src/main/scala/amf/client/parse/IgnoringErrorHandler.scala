package amf.client.parse
import amf.core.annotations.LexicalInformation
import amf.core.errorhandling.AMFErrorHandler
import amf.core.validation.core.ValidationSpecification
import org.mulesoft.lexer.SourceLocation
import org.yaml.model.{SyamlException, YError}

object IgnoringErrorHandler extends AMFErrorHandler {
  override def handle[T](error: YError, defaultValue: T): T = defaultValue
  override def warning(id: ValidationSpecification,
                       node: String,
                       property: Option[String],
                       message: String,
                       lexical: Option[LexicalInformation],
                       location: Option[String]): Unit              = {}
  override def handle(loc: SourceLocation, e: SyamlException): Unit = {}
}
