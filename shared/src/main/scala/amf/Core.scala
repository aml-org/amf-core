package amf

import amf.client.convert.CoreClientConverters._
import amf.client.convert.CoreRegister
import amf.client.environment.{DefaultEnvironment, Environment}
import amf.client.execution.BaseExecutionEnvironment
import amf.client.model.document._
import amf.client.plugins.AMFPlugin
import amf.client.render.Renderer
import amf.client.resolve.Resolver
import amf.client.validate.{AMFValidationReport, Validator}
import amf.core.AMF
import amf.core.registries.AMFPluginsRegistry
import amf.core.unsafe.PlatformSecrets

import scala.concurrent.ExecutionContext

object Core extends PlatformSecrets {

  def init(): ClientFuture[Unit] = init(platform.defaultExecutionEnvironment)

  def init(exec: BaseExecutionEnvironment): ClientFuture[Unit] = {
    implicit val executionContext: ExecutionContext = exec.executionContext
    CoreRegister.register(platform)
    // Init the core component
    AMF.init().asClient
  }

  def generator(vendor: String, mediaType: String): Renderer = new Renderer(vendor, mediaType, None)

  def generator(vendor: String, mediaType: String, env: Environment): Renderer =
    new Renderer(vendor, mediaType, Some(env))

  def resolver(vendor: String): Resolver = new Resolver(vendor)

  def validate(model: BaseUnit,
               profileName: ProfileName,
               env: Environment): ClientFuture[AMFValidationReport] =
    Validator.validate(model, profileName, AMFPluginsRegistry.obtainStaticConfig(), resolved = false)

  def validate(model: BaseUnit, profileName: ProfileName): ClientFuture[AMFValidationReport] =
    validate(model, profileName, DefaultEnvironment())

  def validateResolved(model: BaseUnit, profileName: ProfileName, env: Environment): ClientFuture[AMFValidationReport] = {
    Validator.validate(model, profileName, AMFPluginsRegistry.obtainStaticConfig(), resolved = true)
  }

  def validateResolved(model: BaseUnit, profileName: ProfileName): ClientFuture[AMFValidationReport] =
    validateResolved(model, profileName, DefaultEnvironment())

  def loadValidationProfile(url: String, env: Environment): ClientFuture[ProfileName] =
    Validator.loadValidationProfile(url, env)

  def loadValidationProfile(url: String): ClientFuture[ProfileName] =
    loadValidationProfile(url, DefaultEnvironment())

  def emitShapesGraph(profileName: ProfileName): String =
    Validator.emitShapesGraph(profileName)

  def registerPlugin(plugin: AMFPlugin): Unit = AMF.registerPlugin(plugin)
}
