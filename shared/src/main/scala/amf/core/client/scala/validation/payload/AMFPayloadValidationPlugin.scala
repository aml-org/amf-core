package amf.core.client.scala.validation.payload

import amf.core.client.scala.model.document.PayloadFragment
import amf.core.client.scala.model.domain.Shape
import amf.core.client.scala.validation.{AMFValidationReport, AMFValidationResult}
import amf.core.client.common.validation.{StrictValidationMode, ValidationMode}
import amf.core.internal.validation.ValidationConfiguration

import scala.concurrent.{ExecutionContext, Future}

trait AMFPayloadValidationPlugin {

  val ID: String
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
  val configuration: ValidationConfiguration

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