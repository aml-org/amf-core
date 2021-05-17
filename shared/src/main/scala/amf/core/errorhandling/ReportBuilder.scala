package amf.core.errorhandling
import amf.{MessageStyle, ProfileName}
import amf.core.model.document.BaseUnit
import amf.core.validation._
import amf.plugins.features.validation.ParserSideValidationProfiler

class AmfReportBuilder(model: BaseUnit, profileName: ProfileName) {
  def buildReport(results: Seq[AMFValidationResult]): AMFValidationReport = {
    AMFValidationReport(
        conforms = !results.exists(_.severityLevel == SeverityLevels.VIOLATION),
        model = model.id,
        profile = profileName,
        results = results
    )
  }
}
