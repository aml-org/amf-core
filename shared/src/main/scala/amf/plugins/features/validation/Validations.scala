package amf.plugins.features.validation

import amf.core.unsafe.PlatformSecrets
import amf.core.validation.SeverityLevels.VIOLATION
import amf.core.validation.core.ValidationSpecification
import amf.core.vocabulary.Namespace
import amf.{AmlProfile, ProfileName, ProfileNames}

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
