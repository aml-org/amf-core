package amf.client.parse

import amf.client.convert.CoreClientConverters._
import amf.client.environment.{DefaultEnvironment, Environment}
import amf.client.execution.BaseExecutionEnvironment
import amf.client.model.document.BaseUnit
import amf.client.remod.amfcore.registry.PluginsRegistry
import amf.client.validate.AMFValidationReport
import amf.core.client.ParsingOptions
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.document.{BaseUnit => InternalBaseUnit}
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote.{Cache, Context}
import amf.core.services.{RuntimeCompiler, RuntimeValidator}
import amf.internal.environment
import amf.internal.resource.{ResourceLoader, StringResourceLoader}
import amf.{MessageStyle, ProfileName, RAMLStyle}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.annotation.JSExport

/**
  * Base class for parsers.
  */
class Parser(vendor: String, mediaType: String, private val env: Option[Environment] = None) {

  private var parsedModel: Option[InternalBaseUnit] = None
  private val executionEnvironment: BaseExecutionEnvironment = env match {
    case Some(environment) => environment.executionEnvironment
    case None              => platform.defaultExecutionEnvironment
  }

  private implicit val executionContext: ExecutionContext = executionEnvironment.executionContext

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url : Location of the api.
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  @JSExport
  def parseFileAsync(url: String): ClientFuture[BaseUnit] = parseAsync(url).asClient

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url : Location of the api.
    * @param options: Parsing options
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  @JSExport
  def parseFileAsync(url: String, options: ParsingOptions): ClientFuture[BaseUnit] =
    parseAsync(url, parsingOptions = options).asClient

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param stream: The unit as a string
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  @JSExport
  def parseStringAsync(stream: String): ClientFuture[BaseUnit] =
    parseAsync(DEFAULT_DOCUMENT_URL, Some(fromStream(stream))).asClient

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param stream: The unit as a string
    * @param options: Parsing options
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  @JSExport
  def parseStringAsync(stream: String, options: ParsingOptions): ClientFuture[BaseUnit] =
    parseAsync(DEFAULT_DOCUMENT_URL, Some(fromStream(stream)), parsingOptions = options).asClient

  @JSExport
  def parseStringAsync(url: String, stream: String): ClientFuture[BaseUnit] =
    parseAsync(url, Some(fromStream(url, stream))).asClient

  @JSExport
  def parseStringAsync(url: String, stream: String, options: ParsingOptions): ClientFuture[BaseUnit] =
    parseAsync(url, Some(fromStream(url, stream)), parsingOptions = options).asClient

  /**
    * Generates the validation report for the last parsed model.
    * @param profile the profile to be parsed
    * @param messageStyle if a RAML/OAS profile, this can be set to the preferred error reporting styl
    * @return the AMF validation report
    */
  @JSExport
  def reportValidation(profile: ProfileName, messageStyle: MessageStyle): ClientFuture[AMFValidationReport] =
    report(profile, messageStyle)

  @JSExport
  def reportValidation(profile: ProfileName): ClientFuture[AMFValidationReport] = report(profile)

  /**
    * Generates a custom validation profile as specified in the input validation profile file
    * @param profile the profile to be parsed
    * @param customProfilePath path to the custom profile file
    * @return the AMF validation report
    */
  @JSExport
  def reportCustomValidation(profile: ProfileName, customProfilePath: String): ClientFuture[AMFValidationReport] =
    reportCustomValidationImplementation(profile, customProfilePath)

  private[amf] def parseAsync(url: String,
                              loader: Option[ResourceLoader] = None,
                              parsingOptions: ParsingOptions = ParsingOptions()): Future[InternalBaseUnit] = {

    val environment = {
      val e = internalEnv()
      loader.map(e.add).getOrElse(e)
    }

    RuntimeCompiler(
        url,
        Option(mediaType),
        Some(vendor),
        Context(platform),
        env = environment,
        cache = Cache(),
        parsingOptions = parsingOptions,
        errorHandler = DefaultParserErrorHandler.withRun()
    ) map { model =>
      parsedModel = Some(model)
      model
    }
  }

  private def internalEnv(): environment.Environment =
    env.getOrElse(DefaultEnvironment(executionEnvironment))._internal

  /**
    * Generates the validation report for the last parsed model.
    *
    * @param profileName the profile to be parsed
    * @param messageStyle if a RAML/OAS profile, this can be set to the preferred error reporting style
    * @return the AMF validation report
    */
  private def report(profileName: ProfileName,
                     messageStyle: MessageStyle = RAMLStyle): ClientFuture[AMFValidationReport] = {

    val result = parsedModel.map(
        RuntimeValidator(_, profileName, messageStyle, internalEnv(), resolved = false, executionEnvironment)) match {
      case Some(validation) => validation
      case None             => Future.failed(new Exception("No parsed model or current validation found, cannot validate"))
    }

    result.asClient
  }

  /**
    * Generates a custom validation profile as specified in the input validation profile file
    * @param profileName name of the profile to be parsed
    * @param customProfilePath path to the custom profile file
    * @return the AMF validation report
    */
  private def reportCustomValidationImplementation(profileName: ProfileName,
                                                   customProfilePath: String): ClientFuture[AMFValidationReport] = {
    val result = parsedModel match {
      case Some(model) =>
        for {
          _      <- RuntimeValidator.loadValidationProfile(customProfilePath, errorHandler = UnhandledErrorHandler)
          report <- RuntimeValidator(model, profileName, env = internalEnv())
        } yield {
          report
        }
      case _ => throw new Exception("Cannot validate without parsed model")
    }

    result.asClient
  }

  private def fromStream(url: String, stream: String): ResourceLoader =
    StringResourceLoader(platform.resolvePath(url), stream)

  private def fromStream(stream: String): ResourceLoader = fromStream(DEFAULT_DOCUMENT_URL, stream)

  private val DEFAULT_DOCUMENT_URL = "http://a.ml/amf/default_document"
}
