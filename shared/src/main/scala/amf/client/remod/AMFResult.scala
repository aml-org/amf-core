package amf.client.remod

import amf.core.model.document.{BaseUnit, Document}
import amf.core.validation.{AMFValidationReport, AMFValidationResult}

import scala.concurrent.Future

/**
  *
  * @param bu {@link amf.core.model.document.BaseUnit} returned from AMF parse or transform
  * @param result the resultant {@link amf.core.validation.AMFValidationReport} of the BaseUnit
  */
case class AMFResult(bu: BaseUnit, result: AMFValidationReport) {

  def conforms: Boolean = result.conforms

  def asDocument: Document = bu match {
    case d: Document => d
    case _           => Document()
  }
}
