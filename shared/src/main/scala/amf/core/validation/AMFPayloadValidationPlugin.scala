package amf.core.validation
import amf.client.plugins.{AMFPlugin, StrictValidationMode, ValidationMode}
import amf.client.remod.amfcore.plugins.validate.ValidationConfiguration
import amf.core.model.document.PayloadFragment
import amf.core.model.domain.Shape
import amf.internal.environment.Environment

import scala.concurrent.{ExecutionContext, Future}

trait AMFPayloadValidationPlugin extends AMFPlugin {

  val payloadMediaType: Seq[String]

  def canValidate(shape: Shape, config: ValidationConfiguration): Boolean

  // TODO ARM we can remove the validation mode and handle it on different plugins, o we can put the mode into the options
  def validator(s: Shape,
                config: ValidationConfiguration,
                validationMode: ValidationMode = StrictValidationMode): PayloadValidator

}

trait PayloadValidator {

  val shape: Shape
  val defaultSeverity: String
  val validationMode: ValidationMode
  val config: ValidationConfiguration

  def validate(mediaType: String, payload: String)(
      implicit executionContext: ExecutionContext): Future[AMFValidationReport]

  def validate(payloadFragment: PayloadFragment)(
      implicit executionContext: ExecutionContext): Future[AMFValidationReport]

  def syncValidate(mediaType: String, payload: String): AMFValidationReport

  def isValid(mediaType: String, payload: String)(implicit executionContext: ExecutionContext): Future[Boolean]
}

case class PayloadParsingResult(fragment: PayloadFragment, results: List[AMFValidationResult]) {
  def hasError: Boolean = results.nonEmpty
}
