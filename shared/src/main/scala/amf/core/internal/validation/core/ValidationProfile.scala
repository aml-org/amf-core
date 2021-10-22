package amf.core.internal.validation.core

import amf.core.client.common.validation.{ProfileName, SeverityLevels}
import amf.core.internal.validation.core.ValidationProfile.{SeverityLevel, ValidationName}

import scala.collection.mutable

case class ValidationProfile(name: ProfileName,
                             baseProfile: Option[ProfileName],
                             validations: Seq[ValidationSpecification],
                             severities: SeverityMapping,
                             prefixes: mutable.Map[String, String] = mutable.Map.empty) {

  def reverseNestedConstraintIndex: NestedToParentIndex = NestedToParentIndex(this)

  def validationsWith(severity: SeverityLevel): Seq[ValidationName] = {
    severity match {
      case SeverityLevels.INFO      => severities.info
      case SeverityLevels.WARNING   => severities.warning
      case SeverityLevels.VIOLATION => severities.violation
    }
  }
}

object SeverityMapping {
  val empty: SeverityMapping = SeverityMapping()
}

case class SeverityMapping private (violation: Seq[ValidationName] = Seq.empty,
                                    warning: Seq[ValidationName] = Seq.empty,
                                    info: Seq[ValidationName] = Seq.empty,
                                    disabled: Seq[ValidationName] = Seq.empty,
                                    default: SeverityLevel = SeverityLevels.VIOLATION) {

  def set(validations: Seq[ValidationName], severity: SeverityLevel): SeverityMapping = {
    severity match {
      case SeverityLevels.INFO      => copy(info = validations)
      case SeverityLevels.WARNING   => copy(warning = validations)
      case SeverityLevels.VIOLATION => copy(violation = validations)
    }
  }

  def getSeverityOf(name: ValidationName): Option[String] = {
    if (violation.contains(name)) Some(SeverityLevels.VIOLATION)
    else if (warning.contains(name)) Some(SeverityLevels.WARNING)
    else if (info.contains(name)) Some(SeverityLevels.INFO)
    else None
  }

  def disable(validations: Seq[ValidationName]): SeverityMapping = {
    copy(disabled = validations)
  }

  def concat(mapping: SeverityMapping): SeverityMapping = {
    copy(violation ++ mapping.violation,
         warning ++ mapping.warning,
         info ++ mapping.info,
         disabled ++ mapping.disabled,
         default)
  }
}

object ValidationProfile {
  // Circumvent no-typing
  type ValidationName = String
  type ValidationIri  = String
  type SeverityLevel  = String
}

case class NestedToParentIndex(profile: ValidationProfile) {

  val nestedToParentMap: Map[ValidationName, Seq[ValidationSpecification]] = {
    case class ParentChildPair(parent: ValidationSpecification, child: ValidationName)
    val grouped = profile.validations.toStream
      .filter(_.nested.isDefined)
      .map { parent =>
        val child = parent.nested.get
        ParentChildPair(parent, child)
      }
      .groupBy(_.child)

    grouped.mapValues { pairs =>
      pairs.map(_.parent)
    }
  }
}
