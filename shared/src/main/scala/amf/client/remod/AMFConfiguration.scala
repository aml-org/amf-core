package amf.client.remod

import amf.client.remod.amfcore.config._
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.plugins.parse.AMFParsePlugin
import amf.client.remod.amfcore.registry.AMFRegistry
import amf.core.validation.core.ValidationProfile
import amf.internal.environment.Environment
import amf.internal.reference.UnitCache
import amf.internal.resource.ResourceLoader

import scala.concurrent.ExecutionContext
// all constructors only visible from amf. Users should always use builders or defaults
//TODO: ARM - delete private[amf]
private[amf] class AMFConfiguration(
    private[amf] val resolvers: AMFResolvers,
    private[amf] val errorHandlerProvider: ErrorHandlerProvider,
    private[amf] val registry: AMFRegistry,
    private[amf] val logger: AMFLogger,
    private[amf] val listeners: List[AMFEventListener],
    private[amf] val options: AMFOptions) { // break platform into more specific classes?

  def createClient(): AMFClient = new AMFClient(this)

  def withParsingOptions(parsingOptions: ParsingOptions): AMFConfiguration =
    copy(options = options.copy(parsingOptions = parsingOptions))

  def withRenderOptions(renderOptions: RenderOptions): AMFConfiguration =
    copy(options = options.copy(renderOptions = renderOptions))

  def withErrorHandlerProvider(provider: ErrorHandlerProvider): AMFConfiguration =
    copy(errorHandlerProvider = provider)

  def withResourceLoader(rl: ResourceLoader): AMFConfiguration = copy(resolvers.withResourceLoader(rl))

  def withResourceLoaders(rl: List[ResourceLoader]): AMFConfiguration =
    copy(resolvers = resolvers.withResourceLoaders(rl))

  def withUnitCache(cache: UnitCache): AMFConfiguration =
    copy(resolvers.withCache(cache))

  def withPlugin(amfPlugin: AMFParsePlugin): AMFConfiguration = copy(registry = registry.withPlugin(amfPlugin))

  def withPlugins(plugins: List[AMFPlugin[_]]): AMFConfiguration = copy(registry = registry.withPlugins(plugins))

  // //TODO: ARM - delete
  def removePlugin(id: String): AMFConfiguration = copy(registry = registry.removePlugin(id))

  def withValidationProfile(profile: ValidationProfile): AMFConfiguration =
    copy(registry = registry.withConstraints(profile))

  /**
    * Merges two environments taking into account specific attributes that can be merged.
    * This is currently limited to: registry plugins.
    */
  def merge(other: AMFConfiguration): AMFConfiguration = {
    this.withPlugins(other.getRegistry.getAllPlugins())
  }

  protected def copy(resolvers: AMFResolvers = resolvers,
                     errorHandlerProvider: ErrorHandlerProvider = errorHandlerProvider,
                     registry: AMFRegistry = registry,
                     logger: AMFLogger = logger,
                     listeners: List[AMFEventListener] = Nil,
                     options: AMFOptions = options): AMFConfiguration = {
    new AMFConfiguration(resolvers, errorHandlerProvider, registry, logger, listeners, options)

  }

  private[amf] def getParsingOptions: ParsingOptions        = options.parsingOptions
  private[amf] def getRegistry: AMFRegistry                 = registry
  private[amf] def getResourceLoaders: List[ResourceLoader] = resolvers.resourceLoaders
  private[amf] def getUnitsCache: Option[UnitCache]         = resolvers.unitCache
  private[amf] def getExecutionContext: ExecutionContext    = resolvers.executionContext.executionContext
}

object AMFConfiguration {

  /**
    * Predefined AMF core environment with
    * AMF Resolvers predefined {@link amf.client.remod.amfcore.config.AMFResolvers.predefined()}
    * Default error handler provider that will create a {@link amf.client.parse.DefaultParserErrorHandler}
    * Empty AMF Registry: {@link amf.client.remod.amfcore.registry.AMFRegistry.empty}
    * MutedLogger: {@link amf.client.remod.amfcore.config.MutedLogger}
    * Without Any listener
    */
  def predefined(): AMFConfiguration = {
    new AMFConfiguration(
        AMFResolvers.predefined(),
        DefaultErrorHandlerProvider,
        AMFRegistry.empty,
        MutedLogger,
        Nil,
        AMFOptions.default()
    )
  }

  def fromLegacy(base: AMFConfiguration, legacy: Environment): AMFConfiguration = {
    legacy.maxYamlReferences.foreach { maxValue =>
      base.getParsingOptions.setMaxYamlReferences(maxValue)
    }
    val withLoaders: AMFConfiguration = base.withResourceLoaders(legacy.loaders.toList)
    legacy.resolver.map(unitCache => withLoaders.withUnitCache(unitCache)).getOrElse(withLoaders)
  }
}
