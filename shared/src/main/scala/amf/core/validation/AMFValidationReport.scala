package amf.core.validation

import amf.client.remod.AMFResult
import amf.{AmfProfile, ProfileName, UnknownProfile}
import amf.core.model.document.BaseUnit

case class AMFValidationReport(model: String, profile: ProfileName, results: Seq[AMFValidationResult])
    extends ReportConformance(results) {

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
    new AMFValidationReport(model, profile, results)

  def empty(model: String, profileName: ProfileName): AMFValidationReport = apply(model, profileName, Seq())

  /** used for storing results of parsing and transformation into report for tests */
  private[amf] def unknownProfile(result: AMFResult): AMFValidationReport = {
    val profileName = UnknownProfile
    val model       = result.bu
    new AMFValidationReport(
        model.location().getOrElse(model.id),
        profileName,
        result.results
    )
  }
}

abstract class ReportConformance(results: Seq[AMFValidationResult]) {
  lazy val conforms: Boolean = !results.exists(r => r.severityLevel == SeverityLevels.VIOLATION)
}
