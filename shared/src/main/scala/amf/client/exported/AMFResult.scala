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
    * @return The resultant [[AMFValidationReport]] of the BaseUnit
    */
  def validationResult: AMFValidationReport = _internal.report

  /**
    * @return [[BaseUnit]] returned from AMF parse or transform. It can be:
    *  - A [[BaseUnit]] when parsing/transformation is successful
    *  - The most complete unit that could be built, and an [[AMFValidationReport]] report with errors/warnings found
    */
  def baseUnit: BaseUnit = _internal.bu
}
