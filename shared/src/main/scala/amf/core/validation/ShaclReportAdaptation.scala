package amf.core.validation

import amf.{MessageStyle, OASStyle, ProfileName, RAMLStyle}
import amf.core.model.document.BaseUnit
import amf.core.validation.core.ValidationProfile.SeverityLevel
import amf.core.validation.core.{ValidationReport, ValidationResult, ValidationSpecification}
import amf.core.vocabulary.Namespace

import scala.collection.mutable

trait ShaclReportAdaptation {

  protected def adaptToAmfReport(model: BaseUnit,
                                 profile: ProfileName,
                                 report: ValidationReport,
                                 validations: EffectiveValidations): AMFValidationReport = {
    val amfResults = report.results.flatMap { r =>
      adaptToAmfResult(model, r, profile.messageStyle, validations)
    }
    AMFValidationReport(model.id, profile, amfResults)
  }

  protected def adaptToAmfResult(model: BaseUnit,
                                 result: ValidationResult,
                                 messageStyle: MessageStyle,
                                 validations: EffectiveValidations): Option[AMFValidationResult] = {
    val validationSpecToLook = if (result.sourceShape.startsWith(Namespace.Data.base)) {
      result.sourceShape
        .replace(Namespace.Data.base, "") // this is for custom validations they are all prefixed with the data namespace
    } else {
      result.sourceShape // by default we expect to find a URI here
    }
    val idMapping: mutable.HashMap[String, String] = mutable.HashMap()
    val maybeTargetSpecification: Option[ValidationSpecification] = validations.all.get(validationSpecToLook) match {
      case Some(validationSpec) =>
        idMapping.put(result.sourceShape, validationSpecToLook)
        Some(validationSpec)

      case None =>
        validations.all.find {
          case (v, _) =>
            // processing property shapes Id computed as constraintID + "/prop"

            validationSpecToLook.startsWith(v)
        } match {
          case Some((v, spec)) =>
            idMapping.put(result.sourceShape, v)
            Some(spec)
          case None =>
            if (validationSpecToLook.startsWith("_:")) {
              None
            } else {
              throw new Exception(s"Cannot find validation spec for validation error:\n $result")
            }
        }
    }

    maybeTargetSpecification match {
      case Some(targetSpec) =>
        var message = messageStyle match {
          case RAMLStyle => targetSpec.ramlMessage.getOrElse(targetSpec.message)
          case OASStyle  => targetSpec.oasMessage.getOrElse(targetSpec.message)
          case _         => Option(targetSpec.message).getOrElse(result.message.getOrElse(""))
        }

        if (Option(message).isEmpty || message == "") {
          message = result.message.getOrElse("Constraint violation")
        }

        if (targetSpec.isParserSide && result.message.nonEmpty) {
          message = result.message.get
        }

        val finalId = if (idMapping(result.sourceShape).startsWith("http")) {
          idMapping(result.sourceShape)
        } else {
          Namespace.Data.base + idMapping(result.sourceShape)
        }
        Some(
            AMFValidationResult
              .withShapeId(finalId, AMFValidationResult.fromSHACLValidation(model, message, result.severity, result)))
      case _ => None
    }
  }

  protected def findLevel(id: String,
                          validations: EffectiveValidations,
                          default: String = SeverityLevels.VIOLATION): SeverityLevel =
    validations.findSecurityLevelFor(id).getOrElse(SeverityLevels.unapply(default))

}
