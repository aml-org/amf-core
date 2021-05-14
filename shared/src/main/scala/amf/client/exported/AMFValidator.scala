package amf.client.exported

import amf.ProfileName
import amf.client.model.document.BaseUnit
import amf.client.validate.AMFValidationReport
import amf.client.remod.{AMFValidator => InternalAMFValidator}
import amf.client.convert.CoreClientConverters._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("AMFValidator")
object AMFValidator {
  def validate(bu: BaseUnit, conf: AMFGraphConfiguration): AMFValidationReport =
    InternalAMFValidator.validate(bu, conf)
  def validate(bu: BaseUnit, profileName: ProfileName, conf: AMFGraphConfiguration): AMFValidationReport =
    InternalAMFValidator.validate(bu, profileName, conf)
}
