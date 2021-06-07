package amf.client.plugins

import amf.client.convert.CoreClientConverters._
import amf.client.exported.AMFGraphConfiguration
import amf.client.model.document.PayloadFragment
import amf.client.model.domain.Shape
import amf.client.remod.amfcore.plugins.validate.ValidationConfiguration
import amf.client.validate.AMFValidationReport

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait ClientAMFPayloadValidationPlugin extends ClientAMFPlugin {

  val payloadMediaType: ClientList[String]

  def canValidate(shape: Shape, config: ValidationConfiguration): Boolean

  def validator(s: Shape,
                config: ValidationConfiguration,
                validationMode: ValidationMode = StrictValidationMode): ClientPayloadValidator
}

trait ClientPayloadValidator {

  val shape: Shape
  val defaultSeverity: String
  val validationMode: ValidationMode
  val config: ValidationConfiguration

  def validate(payload: String, mediaType: String)(
      implicit executionContext: ExecutionContext): ClientFuture[AMFValidationReport]

  def validate(payloadFragment: PayloadFragment)(
      implicit executionContext: ExecutionContext): ClientFuture[AMFValidationReport]

  def syncValidate(mediaType: String, payload: String): AMFValidationReport

  def isValid(payload: String, mediaType: String)(implicit executionContext: ExecutionContext): ClientFuture[Boolean]
}
