package amf.core.client.scala

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.validation.{AMFValidationResult, ReportConformance}

/**
  *
  * @param bu [[BaseUnit]] returned from AMF parse or transform. It can be:
  *  - A [[BaseUnit]] when parsing/transformation is successful
  *  - The most complete unit that could be built, and a list of [[AMFValidationResult]] with errors/warnings found
  * @param results list of [[AMFValidationResult]] obtained from AMF parse or transform
  */
case class AMFResult(bu: BaseUnit, results: Seq[AMFValidationResult]) extends AMFObjectResult(bu, results)

class AMFObjectResult(val element: AmfObject, results: Seq[AMFValidationResult]) extends ReportConformance(results)