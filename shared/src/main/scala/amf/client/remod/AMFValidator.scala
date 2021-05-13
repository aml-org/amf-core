package amf.client.remod

import amf.ProfileName
import amf.core.model.document.BaseUnit
import amf.core.validation.AMFValidationReport

object AMFValidator {
  def validate(bu: BaseUnit, conf: AMFGraphConfiguration): AMFValidationReport                           = ???
  def validate(bu: BaseUnit, profileName: ProfileName, conf: AMFGraphConfiguration): AMFValidationReport = ???
}
