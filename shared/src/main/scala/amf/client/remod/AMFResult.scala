package amf.client.remod

import amf.core.model.document.{BaseUnit, Document}
import amf.core.validation.{AMFValidationReport, AMFValidationResult, ReportConformance, SeverityLevels}

import scala.concurrent.Future

/**
  *
  * @param bu [[BaseUnit]] returned from AMF parse or transform. It can be:
  *  - A [[BaseUnit]] when parsing/transformation is successful
  *  - The most complete unit that could be built, and a list of [[AMFValidationResult]] with errors/warnings found
  * @param results list of [[AMFValidationResult]] obtained from AMF parse or transform
  */
case class AMFResult(bu: BaseUnit, results: Seq[AMFValidationResult]) extends ReportConformance {
  lazy val conforms: Boolean = resultsConform(results)
}
