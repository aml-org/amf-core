package amf.core.services

import amf.client.execution.BaseExecutionEnvironment
import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.validate.ValidationConfiguration
import amf.core.errorhandling.AMFErrorHandler
import amf.core.metamodel.Field
import amf.core.model.document.BaseUnit
import amf.core.model.domain.AmfObject
import amf.core.parser.Annotations
import amf.core.rdf.RdfModel
import amf.core.services.RuntimeValidator.CustomShaclFunctions
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.{ValidationProfile, ValidationReport, ValidationSpecification}
import amf.core.validation.{AMFValidationReport, AMFValidationResult, EffectiveValidations}
import amf.internal.environment.Environment
import amf.{AMFStyle, MessageStyle, ProfileName}

import scala.concurrent.{ExecutionContext, Future}

trait ValidationsMerger {
  val parserRun: Int

  def merge(result: AMFValidationResult): Boolean
}

object IgnoreValidationsMerger extends ValidationsMerger {
  override val parserRun: Int = -1

  override def merge(result: AMFValidationResult): Boolean = false
}

case class AllValidationsMerger(parserRun: Int) extends ValidationsMerger {
  override def merge(result: AMFValidationResult): Boolean = true
}

/**
  * Validation of AMF models
  */
trait RuntimeValidator extends PlatformSecrets {

  /**
    * Loads a validation profile from a URL
    */
  def loadValidationProfile(validationProfilePath: String,
                            env: Environment = Environment(),
                            errorHandler: AMFErrorHandler,
                            exec: BaseExecutionEnvironment = platform.defaultExecutionEnvironment): Future[ProfileName]

  def loadValidationProfile(validationProfilePath: String,
                            errorHandler: AMFErrorHandler,
                            exec: BaseExecutionEnvironment): Future[ProfileName] =
    loadValidationProfile(validationProfilePath, Environment(exec), errorHandler, exec)

  /**
    * Generates a JSON-LD graph with the SHACL shapes for the requested profile name
    *
    * @return JSON-LD graph
    */
  def emitShapesGraph(profileName: ProfileName, constraints: Map[String, ValidationProfile]): String

  /**
    * Returns a native RDF model with the SHACL shapes graph
    */
  def shaclModel(validations: Seq[ValidationSpecification],
                 validationFunctionUrl: String,
                 messgeStyle: MessageStyle = AMFStyle): RdfModel

  /**
    * Main validation function returning an AMF validation report linking validation errors
    * for validations in the profile to domain elements in the model
    */
  def validate(model: BaseUnit,
               givenProfile: ProfileName,
               resolved: Boolean,
               validationConfig: ValidationConfiguration): Future[AMFValidationReport]

}

object RuntimeValidator {
  var validatorOption: Option[RuntimeValidator] = None

  def register(runtimeValidator: RuntimeValidator): Unit = {
    validatorOption = Some(runtimeValidator)
  }

  private def validator: RuntimeValidator = {
    validatorOption match {
      case Some(runtimeValidator) => runtimeValidator
      case None                   => throw new Exception("No registered runtime validator")
    }
  }

  def loadValidationProfile(validationProfilePath: String,
                            env: Environment = Environment(),
                            errorHandler: AMFErrorHandler): Future[ProfileName] =
    validator.loadValidationProfile(validationProfilePath, env, errorHandler)

  def loadValidationProfile(validationProfilePath: String,
                            env: Environment,
                            errorHandler: AMFErrorHandler,
                            executionEnvironment: BaseExecutionEnvironment): Future[ProfileName] =
    validator.loadValidationProfile(validationProfilePath, env, errorHandler, executionEnvironment)

  type PropertyInfo = (Annotations, Field)
  // When no property info is provided violation is thrown in domain element level
  type CustomShaclFunction  = (AmfObject, Option[PropertyInfo] => Unit) => Unit
  type CustomShaclFunctions = Map[String, CustomShaclFunction]

  def emitShapesGraph(profileName: ProfileName, constraints: Map[String, ValidationProfile]): String =
    validator.emitShapesGraph(profileName, constraints: Map[String, ValidationProfile])

  def shaclModel(validations: Seq[ValidationSpecification],
                 validationFunctionUrl: String,
                 messageStyle: MessageStyle = AMFStyle): RdfModel =
    validator.shaclModel(validations, validationFunctionUrl, messageStyle)

  def apply(model: BaseUnit,
            profileName: ProfileName,
            resolved: Boolean,
            config: ValidationConfiguration): Future[AMFValidationReport] =
    validator.validate(model, profileName, resolved, config)

}

class ShaclValidationOptions() {
  val filterFields: Field => Boolean = (_: Field) => false
  var messageStyle: MessageStyle     = AMFStyle
  var level: String                  = "partial" // partial | full

  def toRenderOptions: RenderOptions = RenderOptions().withValidation.withFilterFieldsFunc(filterFields)

  def withMessageStyle(style: MessageStyle): ShaclValidationOptions = {
    messageStyle = style
    this
  }

  def withFullValidation(): ShaclValidationOptions = {
    level = "full"
    this
  }

  def withPartialValidation(): ShaclValidationOptions = {
    level = "partial"
    this
  }

  def isPartialValidation: Boolean = level == "partial"
}

object DefaultShaclValidationOptions extends ShaclValidationOptions {}
