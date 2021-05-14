package amf.client.exported

import amf.client.model.document.{BaseUnit, Document}
import amf.client.remod.{AMFResult => InternalAMFResult}

import scala.scalajs.js.annotation.JSExportAll
import amf.client.convert.CoreClientConverters._
import amf.client.validate.AMFValidationReport

@JSExportAll
case class AMFResult(private[amf] val _internal: InternalAMFResult) {

  def conforms: Boolean = _internal.conforms

  /**
    * @return result the resultant {@link amf.client.validate.AMFValidationReport} of the BaseUnit
    */
  def validationResult: AMFValidationReport = _internal.result

  /**
    * @return baseUnit {@link amf.client.model.document.BaseUnit} returned from AMF parse or transform
    */
  def baseUnit: BaseUnit = _internal.bu

  def asDocument: Document = new Document(_internal.asDocument)
}
