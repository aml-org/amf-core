package amf.client.remod

import amf.core.model.document.{BaseUnit, Document}
import amf.core.validation.{AMFValidationReport, AMFValidationResult}

import scala.concurrent.Future

/**
  *
  * @param bu {@link amf.core.model.document.BaseUnit} returned from AMF parse or transform
  * @param report the resultant {@link amf.core.validation.AMFValidationReport} of the BaseUnit
  */
case class AMFResult(bu: BaseUnit, report: AMFValidationReport) {
  def conforms: Boolean = report.conforms
}
