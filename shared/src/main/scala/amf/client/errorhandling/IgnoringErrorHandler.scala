package amf.client.errorhandling
import amf.core.annotations.LexicalInformation
import amf.core.errorhandling.AMFErrorHandler
import amf.core.validation.AMFValidationResult
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
  override def report(result: AMFValidationResult): Unit            = {}
  override def getResults: List[AMFValidationResult]                = Nil
}
