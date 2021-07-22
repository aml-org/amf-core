package amf.core.client.platform.validation

import amf.core.client.platform.AMFGraphConfiguration
import amf.core.client.platform.model.document.BaseUnit
import amf.core.client.common.validation.ProfileName
import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.scala.validation.{AMFValidator => InternalAMFValidator}
import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("AMFValidator")
object AMFValidator {
  def validate(baseUnit: BaseUnit, conf: AMFGraphConfiguration): ClientFuture[AMFValidationReport] = {
    implicit val contextForImplicitConversion: ExecutionContext = conf.getExecutionContext
    InternalAMFValidator.validate(baseUnit, conf).asClient
  }
}
