package amf.client.remod

import amf.core.model.document.{BaseUnit, Document}
import amf.core.validation.{AMFValidationReport, AMFValidationResult}

import scala.concurrent.Future

/**
  *
  * @param bu {@link amf.core.model.document.BaseUnit} returned from AMF parse or transform
  * @param report the resultant {@link amf.core.validation.AMFValidationReport} of the BaseUnit
  * @param bu [[BaseUnit]] returned from AMF parse or transform. It can be:
  *  - A [[BaseUnit]] when parsing/transformation is successful
  *  - The most complete unit that could be built, and an [[AMFValidationReport]] report with errors/warnings found
  * @param result the resultant [[AMFValidationReport]] of the BaseUnit
  */
case class AMFResult(bu: BaseUnit, report: AMFValidationReport) {
  def conforms: Boolean = report.conforms
}
