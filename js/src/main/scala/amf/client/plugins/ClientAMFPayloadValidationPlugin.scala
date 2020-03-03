package amf.client.plugins

import amf.client.convert.CoreClientConverters._
import amf.client.environment.Environment
import amf.client.model.document.PayloadFragment
import amf.client.model.domain.Shape
import amf.client.validate.ValidationReport

import scala.scalajs.js

@js.native
trait ClientAMFPayloadValidationPlugin extends ClientAMFPlugin {

  val payloadMediaType: ClientList[String] = js.native

  def canValidate(shape: Shape, env: Environment): Boolean = js.native

  def validator(s: Shape,
                env: Environment,
                validationMode: ValidationMode = StrictValidationMode): ClientPayloadValidator
}

@js.native
trait ClientPayloadValidator extends js.Object {

  val shape: Shape
  val defaultSeverity: String
  val validationMode: ValidationMode
  val env: Environment

  def validate(payload: String, mediaType: String): ClientFuture[ValidationReport] = js.native

  def validate(payloadFragment: PayloadFragment): ClientFuture[ValidationReport] = js.native

  def syncValidate(payload: String, mediaType: String): ValidationReport = js.native

  def isValid(payload: String, mediaType: String): ClientFuture[Boolean] = js.native
}
