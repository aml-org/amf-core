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
//TODO: ARM - delete private[amf]
private[amf] class AMFGraphConfiguration(
    private[amf] val resolvers: AMFResolvers,
    private[amf] val errorHandlerProvider: ErrorHandlerProvider,
    private[amf] val registry: AMFRegistry,
    private[amf] val logger: AMFLogger,
    private[amf] val listeners: Set[AMFEventListener],
    private[amf] val options: AMFOptions) { // break platform into more specific classes?

  def createClient(): AMFGraphClient = new AMFGraphClient(this)

  def withParsingOptions(parsingOptions: ParsingOptions): AMFGraphConfiguration =
    copy(options = options.copy(parsingOptions = parsingOptions))

  def withRenderOptions(renderOptions: RenderOptions): AMFGraphConfiguration =
    copy(options = options.copy(renderOptions = renderOptions))

  def withErrorHandlerProvider(provider: ErrorHandlerProvider): AMFGraphConfiguration =
    copy(errorHandlerProvider = provider)

  def withResourceLoader(rl: ResourceLoader): AMFGraphConfiguration = copy(resolvers.withResourceLoader(rl))

  def withResourceLoaders(rl: List[ResourceLoader]): AMFGraphConfiguration =
    copy(resolvers = resolvers.withResourceLoaders(rl))

  def withUnitCache(cache: UnitCache): AMFGraphConfiguration =
    copy(resolvers.withCache(cache))

  def withPlugin(amfPlugin: AMFPlugin[_]): AMFGraphConfiguration = copy(registry = registry.withPlugin(amfPlugin))

  def withPlugins(plugins: List[AMFPlugin[_]]): AMFGraphConfiguration = copy(registry = registry.withPlugins(plugins))

  // //TODO: ARM - delete
  def removePlugin(id: String): AMFGraphConfiguration = copy(registry = registry.removePlugin(id))

  def withValidationProfile(profile: ValidationProfile): AMFGraphConfiguration =
    copy(registry = registry.withConstraints(profile))

  def withTransformationPipeline(name: String, pipeline: ResolutionPipeline): AMFGraphConfiguration =
    copy(registry = registry.withTransformationPipeline(name, pipeline))

  def withTransformationPipelines(pipelines: Map[String, ResolutionPipeline]): AMFGraphConfiguration =
    copy(registry = registry.withTransformationPipelines(pipelines))

  /**
    * Merges two environments taking into account specific attributes that can be merged.
    * This is currently limited to: registry plugins, registry transformation pipelines.
    */
  def merge(other: AMFGraphConfiguration): AMFGraphConfiguration = {
    this
      .withPlugins(other.getRegistry.getAllPlugins())
      .withTransformationPipelines(other.registry.transformationPipelines)
  }

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

private[amf] object AMFGraphConfiguration {

  /**
    * Predefined AMF core environment with
    * AMF Resolvers predefined {@link amf.client.remod.amfcore.config.AMFResolvers.predefined()}
    * Default error handler provider that will create a {@link amf.client.parse.DefaultParserErrorHandler}
    * Empty AMF Registry: {@link amf.client.remod.amfcore.registry.AMFRegistry.empty}
    * MutedLogger: {@link amf.client.remod.amfcore.config.MutedLogger}
    * Without Any listener
    */
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

  def predefined(): AMFGraphConfiguration =
    empty()
      .withPlugins(List(AMFGraphParsePlugin, AMFGraphRenderPlugin))
      // we might need to register editing pipeline as well because of legacy behaviour.
      .withTransformationPipeline(PipelineName.from(Amf.name, ResolutionPipeline.DEFAULT_PIPELINE),
                                  new BasicResolutionPipeline())

  def fromLegacy(base: AMFGraphConfiguration, legacy: Environment): AMFGraphConfiguration = {
    legacy.maxYamlReferences.foreach { maxValue =>
      base.getParsingOptions.setMaxYamlReferences(maxValue)
    }
    val withLoaders: AMFGraphConfiguration = base.withResourceLoaders(legacy.loaders.toList)
    legacy.resolver.map(unitCache => withLoaders.withUnitCache(unitCache)).getOrElse(withLoaders)
  }
}
