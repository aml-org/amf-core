package amf.core.validation
import amf.client.plugins.{AMFPlugin, StrictValidationMode, ValidationMode}
import amf.core.model.document.PayloadFragment
import amf.core.model.domain.Shape
import amf.internal.environment.Environment

import scala.concurrent.{ExecutionContext, Future}

trait AMFPayloadValidationPlugin extends AMFPlugin {

  val payloadMediaType: Seq[String]

  def canValidate(shape: Shape, env: Environment): Boolean

  def validator(s: Shape,
                mediaType: String,
                env: Environment,
                validationMode: ValidationMode = StrictValidationMode): PayloadValidator

}

trait PayloadValidator {

  def validate(payload: String)(implicit executionContext: ExecutionContext): Future[AMFValidationReport]

  def validate(payloadFragment: PayloadFragment)(
      implicit executionContext: ExecutionContext): Future[AMFValidationReport]

  def syncValidate(payload: String): AMFValidationReport

  def isValid(payload: String)(implicit executionContext: ExecutionContext): Future[Boolean]
}

case class PayloadParsingResult(fragment: PayloadFragment, results: List[AMFValidationResult]) {
  def hasError: Boolean = results.nonEmpty
}
