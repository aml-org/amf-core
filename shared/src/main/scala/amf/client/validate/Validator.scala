package amf.client.validate

import amf.ProfileName
import amf.client.convert.CoreClientConverters._
import amf.client.environment.{DefaultEnvironment, Environment}
import amf.client.model.document.BaseUnit
import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.plugins.validate.ValidationConfiguration
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.services.RuntimeValidator

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.JSExport

object Validator {

  @JSExport
  def validate(model: BaseUnit,
               profileName: ProfileName,
               config: AMFGraphConfiguration,
               resolved: Boolean = false): ClientFuture[AMFValidationReport] = {
    val validationConfiguration                     = new ValidationConfiguration(config)
    implicit val executionContext: ExecutionContext = validationConfiguration.executionContext
    RuntimeValidator(
        model._internal,
        profileName,
        resolved,
        validationConfiguration
    ).map(report => report).asClient
  }

  @JSExport
  def loadValidationProfile(url: String, env: Environment = DefaultEnvironment()): ClientFuture[ProfileName] = {
    implicit val executionContext: ExecutionContext = env.executionEnvironment.executionContext
    RuntimeValidator
      .loadValidationProfile(url, env._internal, UnhandledErrorHandler, env.executionEnvironment)
      .asClient
  }

  @JSExport
  def emitShapesGraph(profileName: ProfileName): String =
    RuntimeValidator.emitShapesGraph(profileName)
}
