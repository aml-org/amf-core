package amf.client.remod

import amf.core.model.document.{BaseUnit, Document}
import amf.core.validation.{AMFValidationReport, AMFValidationResult}

import scala.concurrent.Future

/**
  *
  * @param bu [[BaseUnit]] returned from AMF parse or transform. It can be:
  *  - A [[BaseUnit]] when parsing/transformation is successful
  *  - The most complete unit that could be built, and an [[AMFValidationReport]] report with errors/warnings found
  * @param result the resultant [[AMFValidationReport]] of the BaseUnit
  */
case class AMFResult(bu: BaseUnit, result: AMFValidationReport) {
  def conforms: Boolean = result.conforms
}
