package amf.client.remod.amfcore.plugins.validate

import amf.ProfileName
import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.ValidationProfile
import amf.core.validation.{AMFValidationReport, EffectiveValidations}

import scala.concurrent.{ExecutionContext, Future}

// TODO: profileName should be opt-in
/**
  * TODO: environment is only there for the AMFPayloadValidationPlugin -> canValidate(Shape, Environment). The Environment is never used (checked XML also).
  */
case class ValidationConfiguration(amfConfig: AMFGraphConfiguration) {

  val eh: AMFErrorHandler                              = amfConfig.errorHandlerProvider.errorHandler()
  val executionContext: ExecutionContext               = amfConfig.getExecutionContext
  val maxYamlReferences: Option[Long]                  = amfConfig.options.parsingOptions.maxYamlReferences
  val constraints: Map[ProfileName, ValidationProfile] = amfConfig.registry.constraintsRules
}

object ValidationConfiguration {
  def predefined(): ValidationConfiguration = new ValidationConfiguration(AMFGraphConfiguration.predefined())
}

case class ValidationOptions(profile: ProfileName,
                             effectiveValidations: EffectiveValidations,
                             config: ValidationConfiguration)
class ValidationInfo(val baseUnit: BaseUnit, val profile: ProfileName)

// TODO: this class shouldn't exist, used to propagate the resolved model
case class ValidationResult(unit: BaseUnit, report: AMFValidationReport)

trait AMFValidatePlugin extends AMFPlugin[BaseUnit] with PlatformSecrets {
  def validate(unit: BaseUnit, options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationResult]
}
