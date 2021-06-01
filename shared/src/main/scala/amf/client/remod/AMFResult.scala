package amf.client.remod

import amf.core.model.document.{BaseUnit, Document}
import amf.core.validation.{AMFValidationReport, AMFValidationResult, ReportConformance, SeverityLevels}

import scala.concurrent.Future

/**
  *
  * @param bu {@link amf.core.model.document.BaseUnit} returned from AMF parse or transform
  * @param results list of {@link amf.core.validation.AMFValidationResult} obtained from AMF parse or transform
  */
case class AMFResult(bu: BaseUnit, results: Seq[AMFValidationResult]) extends ReportConformance {
  lazy val conforms: Boolean = resultsConform(results)
}
