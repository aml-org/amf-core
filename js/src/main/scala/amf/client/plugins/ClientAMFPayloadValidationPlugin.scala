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
                mediaType: String,
                env: Environment,
                validationMode: ValidationMode = StrictValidationMode): ClientPayloadValidator
}

@js.native
trait ClientPayloadValidator extends js.Object {

  def validate(payload: String): ClientFuture[ValidationReport] = js.native

  def validate(payloadFragment: PayloadFragment): ClientFuture[ValidationReport] = js.native

  def syncValidate(payload: String): ValidationReport = js.native

  def isValid(payload: String): ClientFuture[Boolean] = js.native
}
