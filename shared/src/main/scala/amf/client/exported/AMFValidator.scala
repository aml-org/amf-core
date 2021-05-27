package amf.client.exported

import amf.ProfileName
import amf.client.convert.CoreClientConverters._
import amf.client.model.document.BaseUnit
import amf.client.remod.{AMFValidator => InternalAMFValidator}
import amf.client.validate.AMFValidationReport

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("AMFValidator")
object AMFValidator {
  def validate(bu: BaseUnit, conf: AMFGraphConfiguration): ClientFuture[AMFValidationReport] = {
    implicit val contextForImplicitConversion: ExecutionContext = conf.getExecutionContext
    InternalAMFValidator.validate(bu, conf).asClient
  }

  def validate(bu: BaseUnit,
               profileName: ProfileName,
               conf: AMFGraphConfiguration): ClientFuture[AMFValidationReport] = {
    implicit val contextForImplicitConversion: ExecutionContext = conf.getExecutionContext
    InternalAMFValidator.validate(bu, profileName, conf).asClient
  }
}
