package amf.core.client.platform.validation.payload

import amf.core.client.platform.model.document.PayloadFragment
import amf.core.client.platform.validation.AMFValidationReport
import amf.core.client.scala.validation.payload.{AMFShapePayloadValidator => InternalPayloadValidator}
import amf.core.internal.convert.CoreClientConverters._
import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class AMFShapePayloadValidator private[amf] (private[amf] val _internal: InternalPayloadValidator,
                                             private implicit val ec: ExecutionContext) {

  def validate(payload: String): ClientFuture[AMFValidationReport] = _internal.validate(payload).asClient
  def validate(payloadFragment: PayloadFragment): ClientFuture[AMFValidationReport] =
    _internal.validate(payloadFragment).asClient
  def syncValidate(payload: String): AMFValidationReport = _internal.syncValidate(payload)
}
