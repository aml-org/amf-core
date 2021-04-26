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

class AmfStaticReportBuilder(model: BaseUnit, profileName: ProfileName)
    extends AmfReportBuilder(model, profileName)
    with ShaclReportAdaptation {

  val validations: EffectiveValidations =
    EffectiveValidations().someEffective(ParserSideValidationProfiler.parserSideValidationsProfile(profileName))

  def buildFromStatic(): AMFValidationReport = {
    val results =
      model.parserRun.map(StaticErrorCollector.getRun).getOrElse(Nil).map(processAggregatedResult(_, validations))
    super.buildReport(results)
  }

  private def processAggregatedResult(result: AMFValidationResult,
                                      validations: EffectiveValidations): AMFValidationResult = {
    val mappedMessage: String = result.message match {
      case ""   => "Constraint violation"
      case some => some
    }

    val mappedSeverity = findLevel(result.validationId, validations, result.severityLevel)

    new AMFValidationResult(mappedMessage,
                            mappedSeverity,
                            result.targetNode,
                            result.targetProperty,
                            result.validationId,
                            result.position,
                            result.location,
                            result.source)
  }
}
