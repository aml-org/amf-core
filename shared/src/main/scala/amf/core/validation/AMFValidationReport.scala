package amf.core.validation

import amf.{AmfProfile, ProfileName}
import amf.core.model.document.BaseUnit

case class AMFValidationReport(conforms: Boolean,
                               model: String,
                               profile: ProfileName,
                               results: Seq[AMFValidationResult]) {

  private val DefaultMax = 30

  def toString(max: Int): String = {
    val str         = StringBuilder.newBuilder
    val validations = results.take(max).sortWith((c1, c2) => c1.compare(c2) < 0).groupBy(_.severityLevel)

    str.append(s"Model: $model\n")
    str.append(s"Profile: ${profile}\n")
    str.append(s"Conforms? $conforms\n")
    str.append(s"Number of results: ${results.length}\n")

    appendValidations(str, validations, SeverityLevels.VIOLATION)
    appendValidations(str, validations, SeverityLevels.WARNING)
    appendValidations(str, validations, SeverityLevels.INFO)

    str.toString
  }

  private def appendValidations(str: StringBuilder,
                                validations: Map[String, Seq[AMFValidationResult]],
                                level: String): Unit =
    validations.get(level) match {
      case Some(l) =>
        str.append(s"\nLevel: $level\n")
        for { result <- l } {
          str.append(result)
        }
      case None =>
    }

  override def toString: String = toString(DefaultMax)

  def merge(report: AMFValidationReport): AMFValidationReport =
    AMFValidationReport(report.model, report.profile, results ++ report.results)
}

object AMFValidationReport {
  def apply(model: String, profile: ProfileName, results: Seq[AMFValidationResult]) =
    new AMFValidationReport(!results.exists(_.severityLevel == SeverityLevels.VIOLATION), model, profile, results)

  def empty(model: String, profileName: ProfileName): AMFValidationReport = apply(model, profileName, Seq())

  def forModel(model: BaseUnit, results: List[AMFValidationResult]): AMFValidationReport = {
    new AMFValidationReport(
        !results.exists(r => r.severityLevel == SeverityLevels.VIOLATION),
        model.location().getOrElse(model.id),
        model.sourceVendor.map(v => ProfileName.apply(v.name)).getOrElse(AmfProfile),
        results
    )
  }
}
