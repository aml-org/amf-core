package amf.client.remod

import amf.ProfileName
import amf.client.exported.config.{AMFLogger, MutedLogger}
import amf.client.remod.amfcore.config._
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.plugins.parse.SyamlSyntaxParsePlugin
import amf.client.remod.amfcore.plugins.render.{DefaultRenderConfiguration, SyamlSyntaxRenderPlugin}
import amf.client.remod.amfcore.registry.AMFRegistry
import amf.core.annotations.serializable.CoreSerializableAnnotations
import amf.core.entities.CoreEntities
import amf.core.errorhandling.AMFErrorHandler
import amf.core.metamodel.{ModelDefaultBuilder, Obj}
import amf.core.parser.ParserContext
import amf.core.resolution.pipelines.{BasicTransformationPipeline, TransformationPipeline}
import amf.core.validation.core.ValidationProfile
import amf.internal.environment.Environment
import amf.internal.reference.UnitCache
import amf.internal.resource.ResourceLoader
import amf.plugins.document.graph.entities.AMFGraphEntities
import amf.plugins.document.graph.{AMFGraphParsePlugin, AMFGraphRenderPlugin}

import scala.concurrent.ExecutionContext
// all constructors only visible from amf. Users should always use builders or defaults

object AMFGraphConfiguration {

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
    * Predefined AMF core environment with:
    * <ul>
    *   <li>AMF Resolvers predefined {@link amf.client.remod.amfcore.config.AMFResolvers.predefined}</li>
    *   <li>Default error handler provider that will create a {@link amf.client.parse.DefaultErrorHandler}</li>
    *   <li>Empty {@link amf.client.remod.amfcore.registry.AMFRegistry}</li>
    *   <li>MutedLogger: {@link amf.client.remod.amfcore.config.MutedLogger}</li>
    *   <li>Without Any listener</li>
    * </ul>
    */
  def predefined(): AMFGraphConfiguration = {
    new AMFGraphConfiguration(
        AMFResolvers.predefined(),
        DefaultErrorHandlerProvider,
        AMFRegistry.empty
          .withEntities(CoreEntities.entities ++ AMFGraphEntities.entities)
          .withAnnotations(CoreSerializableAnnotations.annotations),
        MutedLogger,
        Set.empty,
        AMFOptions.default()
    ).withPlugins(List(AMFGraphParsePlugin, AMFGraphRenderPlugin, SyamlSyntaxParsePlugin, SyamlSyntaxRenderPlugin))
      // we might need to register editing pipeline as well because of legacy behaviour.
      .withTransformationPipeline(BasicTransformationPipeline())
  }

  def fromLegacy(base: AMFGraphConfiguration, legacy: Environment): AMFGraphConfiguration = {
    legacy.maxYamlReferences.foreach { maxValue =>
      base.getParsingOptions.setMaxYamlReferences(maxValue)
    }
    val withLoaders: AMFGraphConfiguration = base.withResourceLoaders(legacy.loaders.toList)
    legacy.resolver.map(unitCache => withLoaders.withUnitCache(unitCache)).getOrElse(withLoaders)
  }
  //TODO: ARM remove
  private[amf] def fromParseCtx(ctx: ParserContext) = fromEH(ctx.eh)

  private[amf] def fromEH(eh: AMFErrorHandler) = {
    AMFGraphConfiguration.predefined().withErrorHandlerProvider(() => eh)
  }
}

/**
  * Base AMF configuration object
  * @param resolvers {@link amf.client.remod.amfcore.config.AMFResolvers}
  * @param errorHandlerProvider {@link amf.client.remod.ErrorHandlerProvider}
  * @param registry {@link amf.client.remod.amfcore.registry.AMFRegistry}
  * @param logger {@link amf.client.remod.amfcore.config.AMFLogger}
  * @param listeners a Set of {@link amf.client.remod.amfcore.config.AMFEventListener}
  * @param options {@link amf.client.remod.amfcore.config.AMFOptions}
  */
class AMFGraphConfiguration private[amf] (override private[amf] val resolvers: AMFResolvers,
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

  def withEntities(entities: Map[String, ModelDefaultBuilder]): AMFGraphConfiguration = super._withEntities(entities)
  // //TODO: ARM - delete
  def removePlugin(id: String): AMFGraphConfiguration = super._removePlugin(id)

  def withValidationProfile(profile: ValidationProfile): AMFGraphConfiguration =
    super._withValidationProfile(profile)

  // TODO - ARM: Should be erased as configuration should be incremental, not decremental
  def removeValidationProfile[T](name: ProfileName) = super._removeValidationProfile[T](name)

  def withTransformationPipeline(pipeline: TransformationPipeline): AMFGraphConfiguration =
    super._withTransformationPipeline(pipeline)

  /**
    * AMF internal method just to facilitate the construction
    * @param pipelines a list of {@link amf.core.resolution.pipelines.TransformationPipeline}
    * @return
    */
  private[amf] def withTransformationPipelines(pipelines: List[TransformationPipeline]): AMFGraphConfiguration =
    super._withTransformationPipelines(pipelines)

  def withEventListener(listener: AMFEventListener): AMFGraphConfiguration = super._withEventListener(listener)

  def withLogger(logger: AMFLogger): AMFGraphConfiguration = super._withLogger(logger)

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

  private[amf] lazy val parseConfiguration  = ParseConfiguration(this)
  private[amf] lazy val renderConfiguration = DefaultRenderConfiguration(this)

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

  protected def _withEntities[T](entities: Map[String, ModelDefaultBuilder]): T =
    copy(registry = registry.withEntities(entities)).asInstanceOf[T]

  // //TODO: ARM - delete
  protected def _removePlugin[T](id: String): T = copy(registry = registry.removePlugin(id)).asInstanceOf[T]

  protected def _withEventListener[T](listener: AMFEventListener): T =
    copy(listeners = listeners + listener).asInstanceOf[T]

  protected def _withLogger[T](logger: AMFLogger): T =
    copy(logger = logger).asInstanceOf[T]

  // TODO - ARM: Should be erased as configuration should be incremental, not decremental
  protected def _removeValidationProfile[T](name: ProfileName): T =
    copy(registry = registry.removeConstraints(name)).asInstanceOf[T]

  protected def _withValidationProfile[T](profile: ValidationProfile): T =
    copy(registry = registry.withConstraints(profile)).asInstanceOf[T]

  protected def _withTransformationPipeline[T](pipeline: TransformationPipeline): T =
    copy(registry = registry.withTransformationPipeline(pipeline)).asInstanceOf[T]

  protected def _withTransformationPipelines[T](pipelines: List[TransformationPipeline]): T =
    copy(registry = registry.withTransformationPipelines(pipelines)).asInstanceOf[T]

  protected def _merge[T <: BaseAMFConfigurationSetter](other: T): T = {
    this
      ._withPlugins(other.registry.getAllPlugins())
      .asInstanceOf[T]
      ._withTransformationPipelines(other.registry.transformationPipelines.values.toList)
      .asInstanceOf[T]
  }

  protected def copy(resolvers: AMFResolvers = resolvers,
                     errorHandlerProvider: ErrorHandlerProvider = errorHandlerProvider,
                     registry: AMFRegistry = registry,
                     logger: AMFLogger = logger,
                     listeners: Set[AMFEventListener] = Set.empty,
                     options: AMFOptions = options): BaseAMFConfigurationSetter

}
