package amf.client.remod

import amf.client.remod.amfcore.config._
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.plugins.parse.AMFParsePlugin
import amf.client.remod.amfcore.registry.AMFRegistry
import amf.client.remod.amfcore.resolution.{PipelineInfo, PipelineName}
import amf.core.remote.Amf
import amf.core.resolution.pipelines.{BasicResolutionPipeline, ResolutionPipeline}
import amf.core.validation.core.ValidationProfile
import amf.internal.environment.Environment
import amf.internal.reference.UnitCache
import amf.internal.resource.ResourceLoader
import amf.plugins.document.graph.AMFGraphPlugin.ID
import amf.plugins.document.graph.{AMFGraphParsePlugin, AMFGraphRenderPlugin}

import scala.concurrent.ExecutionContext
// all constructors only visible from amf. Users should always use builders or defaults

private[amf] object AMFGraphConfiguration {

  def empty(): AMFGraphConfiguration = {
    new AMFGraphConfiguration(
        AMFResolvers.predefined(),
        DefaultErrorHandlerProvider,
        AMFRegistry.empty,
        MutedLogger,
        Set.empty,
        AMFOptions.default()
    )
  }

  /**
    * Predefined AMF core environment with
    * AMF Resolvers predefined {@link amf.client.remod.amfcore.config.AMFResolvers.predefined()}
    * Default error handler provider that will create a {@link amf.client.parse.DefaultParserErrorHandler}
    * Empty AMF Registry: {@link amf.client.remod.amfcore.registry.AMFRegistry.empty}
    * MutedLogger: {@link amf.client.remod.amfcore.config.MutedLogger}
    * Without Any listener
    */
  def predefined(): AMFGraphConfiguration = {
    new AMFGraphConfiguration(
        AMFResolvers.predefined(),
        DefaultErrorHandlerProvider,
        AMFRegistry.empty,
        MutedLogger,
        Set.empty,
        AMFOptions.default()
    )
  }

  def fromLegacy(base: AMFGraphConfiguration, legacy: Environment): AMFGraphConfiguration = {
    legacy.maxYamlReferences.foreach { maxValue =>
      base.getParsingOptions.setMaxYamlReferences(maxValue)
    }
    val withLoaders: AMFGraphConfiguration = base.withResourceLoaders(legacy.loaders.toList)
    legacy.resolver.map(unitCache => withLoaders.withUnitCache(unitCache)).getOrElse(withLoaders)
  }
}

//TODO: ARM - delete private[amf]
private[amf] class AMFGraphConfiguration(override private[amf] val resolvers: AMFResolvers,
                                         override private[amf] val errorHandlerProvider: ErrorHandlerProvider,
                                         override private[amf] val registry: AMFRegistry,
                                         override private[amf] val logger: AMFLogger,
                                         override private[amf] val listeners: Set[AMFEventListener],
                                         override private[amf] val options: AMFOptions)
    extends BaseAMFConfigurationSetter(resolvers, errorHandlerProvider, registry, logger, listeners, options) { // break platform into more specific classes?

  def createClient(): AMFGraphClient = new AMFGraphClient(this)

  def withParsingOptions(parsingOptions: ParsingOptions): AMFGraphConfiguration =
    super._withParsingOptions(parsingOptions)

  def withRenderOptions(renderOptions: RenderOptions): AMFGraphConfiguration =
    super._withRenderOptions(renderOptions)

  def withErrorHandlerProvider(provider: ErrorHandlerProvider): AMFGraphConfiguration =
    super._withErrorHandlerProvider(provider)

  def withResourceLoader(rl: ResourceLoader): AMFGraphConfiguration = super._withResourceLoader(rl)

  def withResourceLoaders(rl: List[ResourceLoader]): AMFGraphConfiguration =
    super._withResourceLoaders(rl)

  def withUnitCache(cache: UnitCache): AMFGraphConfiguration =
    super._withUnitCache(cache)

  def withPlugin(amfPlugin: AMFPlugin[_]): AMFGraphConfiguration = super._withPlugin(amfPlugin)

  def withPlugins(plugins: List[AMFPlugin[_]]): AMFGraphConfiguration = super._withPlugins(plugins)

  // //TODO: ARM - delete
  def removePlugin(id: String): AMFGraphConfiguration = super._removePlugin(id)

  def withValidationProfile(profile: ValidationProfile): AMFGraphConfiguration =
    super._withValidationProfile(profile)

  def withTransformationPipeline(name: String, pipeline: ResolutionPipeline): AMFGraphConfiguration =
    super._withTransformationPipeline(name, pipeline)

  /**
    * AMF internal method just to facilitate the construction
    * @param pipelines
    * @return
    */
  private[amf] def withTransformationPipelines(pipelines: Map[String, ResolutionPipeline]): AMFGraphConfiguration =
    super._withTransformationPipelines(pipelines)

  /**
    * Merges two environments taking into account specific attributes that can be merged.
    * This is currently limited to: registry plugins, registry transformation pipelines.
    */
  def merge(other: AMFGraphConfiguration): AMFGraphConfiguration = super._merge(other)

  protected def copy(resolvers: AMFResolvers = resolvers,
                     errorHandlerProvider: ErrorHandlerProvider = errorHandlerProvider,
                     registry: AMFRegistry = registry,
                     logger: AMFLogger = logger,
                     listeners: Set[AMFEventListener] = Set.empty,
                     options: AMFOptions = options): AMFGraphConfiguration = {
    new AMFGraphConfiguration(resolvers, errorHandlerProvider, registry, logger, listeners, options)

  }

  private[amf] def getParsingOptions: ParsingOptions        = options.parsingOptions
  private[amf] def getRegistry: AMFRegistry                 = registry
  private[amf] def getResourceLoaders: List[ResourceLoader] = resolvers.resourceLoaders
  private[amf] def getUnitsCache: Option[UnitCache]         = resolvers.unitCache
  private[amf] def getExecutionContext: ExecutionContext    = resolvers.executionContext.executionContext
}

sealed abstract class BaseAMFConfigurationSetter(private[amf] val resolvers: AMFResolvers,
                                                 private[amf] val errorHandlerProvider: ErrorHandlerProvider,
                                                 private[amf] val registry: AMFRegistry,
                                                 private[amf] val logger: AMFLogger,
                                                 private[amf] val listeners: Set[AMFEventListener],
                                                 private[amf] val options: AMFOptions) {
  protected def _withParsingOptions[T](parsingOptions: ParsingOptions): T =
    copy(options = options.copy(parsingOptions = parsingOptions)).asInstanceOf[T]

  protected def _withRenderOptions[T](renderOptions: RenderOptions): T =
    copy(options = options.copy(renderOptions = renderOptions)).asInstanceOf[T]

  protected def _withErrorHandlerProvider[T](provider: ErrorHandlerProvider): T =
    copy(errorHandlerProvider = provider).asInstanceOf[T]

  protected def _withResourceLoader[T](rl: ResourceLoader): T = copy(resolvers.withResourceLoader(rl)).asInstanceOf[T]

  protected def _withResourceLoaders[T](rl: List[ResourceLoader]): T =
    copy(resolvers = resolvers.withResourceLoaders(rl)).asInstanceOf[T]

  protected def _withUnitCache[T](cache: UnitCache): T =
    copy(resolvers.withCache(cache)).asInstanceOf[T]

  protected def _withPlugin[T](amfPlugin: AMFPlugin[_]): T =
    copy(registry = registry.withPlugin(amfPlugin)).asInstanceOf[T]

  protected def _withPlugins[T](plugins: List[AMFPlugin[_]]): T =
    copy(registry = registry.withPlugins(plugins)).asInstanceOf[T]

  // //TODO: ARM - delete
  protected def _removePlugin[T](id: String): T = copy(registry = registry.removePlugin(id)).asInstanceOf[T]

  protected def _withValidationProfile[T](profile: ValidationProfile): T =
    copy(registry = registry.withConstraints(profile)).asInstanceOf[T]

  protected def _withTransformationPipeline[T](name: String, pipeline: ResolutionPipeline): T =
    copy(registry = registry.withTransformationPipeline(name, pipeline)).asInstanceOf[T]

  protected def _withTransformationPipelines[T](pipelines: Map[String, ResolutionPipeline]): T =
    copy(registry = registry.withTransformationPipelines(pipelines)).asInstanceOf[T]

  protected def _merge[T <: BaseAMFConfigurationSetter](other: T): T = {
    this
      ._withPlugins(other.registry.getAllPlugins())
      .asInstanceOf[T]
      ._withTransformationPipelines(other.registry.transformationPipelines)
      .asInstanceOf[T]
  }

  protected def copy(resolvers: AMFResolvers = resolvers,
                     errorHandlerProvider: ErrorHandlerProvider = errorHandlerProvider,
                     registry: AMFRegistry = registry,
                     logger: AMFLogger = logger,
                     listeners: Set[AMFEventListener] = Set.empty,
                     options: AMFOptions = options): BaseAMFConfigurationSetter

}
