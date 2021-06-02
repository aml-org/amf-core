package amf.core.errorhandling
import amf.{MessageStyle, ProfileName}
import amf.core.model.document.BaseUnit
import amf.core.validation._

class AmfReportBuilder(model: BaseUnit, profileName: ProfileName) {
  def buildReport(results: Seq[AMFValidationResult]): AMFValidationReport = {
    AMFValidationReport(
        model = model.id,
        profile = profileName,
        results = results
    )
  }
}
