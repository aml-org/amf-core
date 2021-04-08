package amf.plugins.features.validation

import amf.core.unsafe.PlatformSecrets
import amf.core.validation.SeverityLevels.VIOLATION
import amf.core.validation.core.ValidationSpecification
import amf.core.vocabulary.Namespace
import amf.{AmlProfile, ProfileName, ProfileNames}

/** Access parser, resolution and render validations together. */
object Validations extends PlatformSecrets {

  type ConstraintSeverityOverrides = Map[String, Map[ProfileName, String]]

  val validations: List[ValidationSpecification] = platform.validations.toList

  def severityLevelOf(id: String, profile: ProfileName): String =
    severityLevelsOfConstraints
      .getOrElse(id, default)
      .getOrElse(profile, VIOLATION)

  protected[amf] lazy val severityLevelsOfConstraints: ConstraintSeverityOverrides = validations.foldLeft(levels) {
    (acc, validation) =>
      if (acc.contains(validation.id)) acc
      else acc + (validation.id -> default)
  }

  private val levels: ConstraintSeverityOverrides          = platform.securityLevelOverrides.toMap
  private lazy val default                                 = all(VIOLATION)
  protected def all(lvl: String): Map[ProfileName, String] = ProfileNames.specProfiles.map(_ -> lvl).toMap
}

trait Validations {
  protected def validation(id: String,
                           message: String,
                           ramlMessage: Option[String] = None,
                           oasMessage: Option[String] = None): ValidationSpecification =
    ValidationSpecification(
        name = (namespace + id).iri(),
        message = message,
        ramlMessage = ramlMessage,
        oasMessage = oasMessage,
        targetInstance = Seq(specification)
    )

  protected def all(lvl: String): Map[ProfileName, String] =
    ProfileNames.specProfiles.map(_ -> lvl).toMap + (AmlProfile -> lvl)

  val specification: String
  val namespace: Namespace
  val validations: List[ValidationSpecification]
  val levels: Map[String, Map[ProfileName, String]]
}
