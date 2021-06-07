package amf.client.plugins

import amf.client.convert.CoreClientConverters._
import amf.client.model.document.PayloadFragment
import amf.client.model.domain.Shape
import amf.client.remod.amfcore.plugins.validate.ValidationConfiguration
import amf.client.validate.AMFValidationReport

import scala.scalajs.js

@js.native
trait ClientAMFPayloadValidationPlugin extends ClientAMFPlugin {

  val payloadMediaType: ClientList[String] = js.native

  def canValidate(shape: Shape, config: ValidationConfiguration): Boolean = js.native

  def validator(s: Shape,
                config: ValidationConfiguration,
                validationMode: ValidationMode = StrictValidationMode): ClientPayloadValidator
}

@js.native
trait ClientPayloadValidator extends js.Object {

  val shape: Shape
  val defaultSeverity: String
  val validationMode: ValidationMode
  val config: ValidationConfiguration

  def validate(payload: String, mediaType: String): ClientFuture[AMFValidationReport] = js.native

  def validate(payloadFragment: PayloadFragment): ClientFuture[AMFValidationReport] = js.native

  def syncValidate(payload: String, mediaType: String): AMFValidationReport = js.native

  def isValid(payload: String, mediaType: String): ClientFuture[Boolean] = js.native
}
