package amf.core.validation

import amf.core.validation.core.ValidationProfile.{SeverityLevel, ValidationIri}
import amf.core.validation.core.{ProfileIndex, ValidationProfile, ValidationSpecification}
import amf.core.vocabulary.Namespace

import scala.collection.mutable

class EffectiveValidations(val effective: mutable.HashMap[String, ValidationSpecification] = mutable.HashMap(),
                           val all: mutable.HashMap[String, ValidationSpecification] = mutable.HashMap()) {

  private val SEVERITY_LEVELS                                                   = Seq(SeverityLevels.INFO, SeverityLevels.WARNING, SeverityLevels.VIOLATION)
  private val severityLevelIndex: mutable.HashMap[ValidationIri, SeverityLevel] = mutable.HashMap.empty

  def findSecurityLevelFor(id: ValidationIri): Option[SeverityLevel] = severityLevelIndex.get(id)

  def someEffective(profile: ValidationProfile): EffectiveValidations = {
    val index = profile.index
    // we aggregate all of the validations to the total validations map
    profile.validations.foreach { update }

    SEVERITY_LEVELS.foreach { level =>
      profile.validationsWith(level).foreach { validation =>
        setLevel(validation, level)
        indexedParentsOf(validation, index).foreach { parent =>
          effective.put(parent.id, parent)
        }
      }
    }

    profile.severities.disabled foreach { id =>
      val validationIri: ValidationIri = toIri(id)
      this.effective.remove(validationIri)
    }

    this
  }

  private def indexedParentsOf(validation: String, index: ProfileIndex) =
    index.parents.get(toIri(validation)).toSeq.flatten

  private def update(other: ValidationSpecification): Unit = {
    all.get(other.name) match {
      case Some(added) => all.update(other.name, other withTargets added)
      case None        => all += other.name -> other
    }
  }

  private def setLevel(id: String, targetLevel: SeverityLevel): Unit = {
    val validationIri: ValidationIri = toIri(id)
    all.get(validationIri) match {
      case Some(validation) =>
        severityLevelIndex.update(validationIri, targetLevel)
        effective += (validationIri -> validation)
      case None => // Ignore
    }
  }

  private def toIri(id: String): ValidationIri = {
    if (!isIri(id)) {
      Namespace.staticAliases.expand(id.replace(".", ":")).iri()
    } else {
      id
    }
  }

  private def isIri(id: String) = id.startsWith("https://") || id.startsWith("file:/") || id.startsWith("http://")

}

object EffectiveValidations {
  def apply() = new EffectiveValidations()
}
