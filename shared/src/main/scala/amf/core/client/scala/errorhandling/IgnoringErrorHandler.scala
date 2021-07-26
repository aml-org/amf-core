package amf.core.client.scala.errorhandling
import amf.core.client.scala.validation.AMFValidationResult
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.validation.core.ValidationSpecification
import org.mulesoft.lexer.SourceLocation
import org.yaml.model.{SyamlException, YError}

object IgnoringErrorHandler extends AMFErrorHandler {
  override def warning(id: ValidationSpecification,
                       node: String,
                       property: Option[String],
                       message: String,
                       lexical: Option[LexicalInformation],
                       location: Option[String]): Unit              = {}
  override def report(result: AMFValidationResult): Unit            = {}
  override def getResults: List[AMFValidationResult]                = Nil
}
