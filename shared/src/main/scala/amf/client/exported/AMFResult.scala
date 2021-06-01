package amf.client.exported

import amf.client.model.document.{BaseUnit, Document}
import amf.client.remod.{AMFResult => InternalAMFResult}

import scala.scalajs.js.annotation.JSExportAll
import amf.client.convert.CoreClientConverters._
import amf.client.validate.{ValidationResult}

@JSExportAll
case class AMFResult(private[amf] val _internal: InternalAMFResult) {

  def conforms: Boolean = _internal.conforms

  /**
    * @return list of the resultant {@link amf.client.validate.ValidationResult} of the BaseUnit
    */
  def results: ClientList[ValidationResult] = _internal.results.asClient

  /**
    * @return baseUnit {@link amf.client.model.document.BaseUnit} returned from AMF parse or transform
    */
  def baseUnit: BaseUnit = _internal.bu
}
