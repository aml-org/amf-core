package amf.core.client.scala

import amf.core.client.common.validation.ProfileName
import amf.core.client.scala.config._
import amf.core.client.scala.errorhandling.{AMFErrorHandler, DefaultErrorHandlerProvider, ErrorHandlerProvider}
import amf.core.client.scala.execution.ExecutionEnvironment
import amf.core.client.scala.model.domain.AnnotationGraphLoader
import amf.core.client.scala.resource.ResourceLoader
import amf.core.client.scala.transform.TransformationPipeline
import amf.core.internal.annotations.serializable.CoreSerializableAnnotations
import amf.core.internal.convert.CoreRegister
import amf.core.internal.entities.CoreEntities
import amf.core.internal.metamodel.ModelDefaultBuilder
import amf.core.internal.parser.CompilerConfiguration
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.plugins.document.graph.entities.DataNodeEntities
import amf.core.internal.plugins.parse.{AMFGraphParsePlugin, DomainParsingFallback}
import amf.core.internal.plugins.render.{AMFGraphRenderPlugin, DefaultRenderConfiguration}
import amf.core.internal.plugins.syntax.{SyamlSyntaxParsePlugin, SyamlSyntaxRenderPlugin}
import amf.core.internal.registries.AMFRegistry
import amf.core.internal.resource.AMFResolvers
import amf.core.internal.transform.pipelines.BasicTransformationPipeline
import amf.core.internal.validation.core.ValidationProfile
import amf.core.internal.validation.{EffectiveValidations, ValidationConfiguration}

import scala.concurrent.ExecutionContext
// all constructors only visible from amf. Users should always use builders or defaults

object AMFGraphConfiguration {

  def empty(): AMFGraphConfiguration = {
    new AMFGraphConfiguration(
        AMFResolvers.predefined(),
        DefaultErrorHandlerProvider,
        AMFRegistry.empty,
        Set.empty,
        AMFOptions.default()
    )
  }

  /**
    * Predefined AMF core environment with:
    *   - AMF Resolvers [[AMFResolvers.predefined predefined]]
    *   - Default error handler provider that will create a [[amf.core.client.scala.errorhandling.DefaultErrorHandler]]
    *   - Empty [[AMFRegistry]]
    *   - Without Any listener
    */
  def predefined(): AMFGraphConfiguration = {
    CoreRegister.register() // TODO ARM remove when APIMF-3000 is done
    new AMFGraphConfiguration(
        AMFResolvers.predefined(),
        DefaultErrorHandlerProvider,
        AMFRegistry.empty
          .withEntities(CoreEntities.entities ++ DataNodeEntities.entities)
          .withAnnotations(CoreSerializableAnnotations.annotations),
        Set.empty,
        AMFOptions.default()
    ).withPlugins(List(AMFGraphParsePlugin, AMFGraphRenderPlugin, SyamlSyntaxParsePlugin, SyamlSyntaxRenderPlugin))
      .withTransformationPipeline(BasicTransformationPipeline())
  }

  private[amf] def fromEH(eh: AMFErrorHandler) = {
    AMFGraphConfiguration.predefined().withErrorHandlerProvider(() => eh)
  }
}

/**
  * Base AMF configuration object
  *
  * @param resolvers            [[AMFResolvers]]
  * @param errorHandlerProvider [[ErrorHandlerProvider]]
  * @param registry             [[AMFRegistry]]
  * @param listeners            a Set of [[AMFEventListener]]
  * @param options              [[AMFOptions]]
  */
class AMFGraphConfiguration private[amf] (override private[amf] val resolvers: AMFResolvers,
                                          override private[amf] val errorHandlerProvider: ErrorHandlerProvider,
                                          override private[amf] val registry: AMFRegistry,
                                          override private[amf] val listeners: Set[AMFEventListener],
                                          override private[amf] val options: AMFOptions)
    extends BaseAMFConfigurationSetter(resolvers, errorHandlerProvider, registry, listeners, options) { // break platform into more specific classes?

  /** Contains common AMF graph operations associated to documents */
  def baseUnitClient(): AMFGraphBaseUnitClient = new AMFGraphBaseUnitClient(this)

  /** Contains functionality associated with specific elements of the AMF model */
  def elementClient(): AMFGraphElementClient = new AMFGraphElementClient(this)

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

  def withFallback(plugin: DomainParsingFallback): AMFGraphConfiguration = super._withFallback(plugin)

  def withPlugin(amfPlugin: AMFPlugin[_]): AMFGraphConfiguration = super._withPlugin(amfPlugin)

  def withPlugins(plugins: List[AMFPlugin[_]]): AMFGraphConfiguration = super._withPlugins(plugins)

  private[amf] def withEntities(entities: Map[String, ModelDefaultBuilder]): AMFGraphConfiguration =
    super._withEntities(entities)

  private[amf] def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): AMFGraphConfiguration =
    super._withAnnotations(annotations)

  private[amf] def withValidationProfile(profile: ValidationProfile): AMFGraphConfiguration =
    super._withValidationProfile(profile)

  // Keep AMF internal, done to avoid recomputing validations every time a config is requested
  private[amf] def withValidationProfile(profile: ValidationProfile,
                                         effective: EffectiveValidations): AMFGraphConfiguration =
    super._withValidationProfile(profile, effective)

  def withTransformationPipeline(pipeline: TransformationPipeline): AMFGraphConfiguration =
    super._withTransformationPipeline(pipeline)

  /**
    * AMF internal method just to facilitate the construction
    *
    * @param pipelines a list of [[TransformationPipeline]]
    * @return
    */
  private[amf] def withTransformationPipelines(pipelines: List[TransformationPipeline]): AMFGraphConfiguration =
    super._withTransformationPipelines(pipelines)

  def withEventListener(listener: AMFEventListener): AMFGraphConfiguration = super._withEventListener(listener)

  def withExecutionEnvironment(executionEnv: ExecutionEnvironment): AMFGraphConfiguration =
    super._withExecutionEnvironment(executionEnv)

  protected def copy(resolvers: AMFResolvers = resolvers,
                     errorHandlerProvider: ErrorHandlerProvider = errorHandlerProvider,
                     registry: AMFRegistry = registry,
                     listeners: Set[AMFEventListener] = Set.empty,
                     options: AMFOptions = options): AMFGraphConfiguration = {
    new AMFGraphConfiguration(resolvers, errorHandlerProvider, registry, listeners, options)
  }

  private[amf] def emptyEntities(): AMFGraphConfiguration   = super._emptyEntities()
  private[amf] def getParsingOptions: ParsingOptions        = options.parsingOptions
  private[amf] def getRegistry: AMFRegistry                 = registry
  private[amf] def getResourceLoaders: List[ResourceLoader] = resolvers.resourceLoaders
  private[amf] def getUnitsCache: Option[UnitCache]         = resolvers.unitCache
  private[amf] def getExecutionContext: ExecutionContext    = resolvers.executionEnv.context

  private[amf] def compilerConfiguration   = CompilerConfiguration(this)
  private[amf] def renderConfiguration     = DefaultRenderConfiguration(this)
  private[amf] def validationConfiguration = new ValidationConfiguration(this)

}

sealed abstract class BaseAMFConfigurationSetter(private[amf] val resolvers: AMFResolvers,
                                                 private[amf] val errorHandlerProvider: ErrorHandlerProvider,
                                                 private[amf] val registry: AMFRegistry,
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

  protected def _withFallback[T](plugin: DomainParsingFallback): T =
    copy(registry = registry.withFallback(plugin)).asInstanceOf[T]

  protected def _withPlugins[T](plugins: List[AMFPlugin[_]]): T =
    copy(registry = registry.withPlugins(plugins)).asInstanceOf[T]

  protected def _withEntities[T](entities: Map[String, ModelDefaultBuilder]): T =
    copy(registry = registry.withEntities(entities)).asInstanceOf[T]

  protected def _emptyEntities[T](): T =
    copy(registry = registry.emptyEntities()).asInstanceOf[T]

  protected def _withAnnotations[T](ann: Map[String, AnnotationGraphLoader]): T =
    copy(registry = registry.withAnnotations(ann)).asInstanceOf[T]

  protected def _withEventListener[T](listener: AMFEventListener): T =
    copy(listeners = listeners + listener).asInstanceOf[T]

  protected def _withValidationProfile[T](profile: ValidationProfile): T =
    copy(registry = registry.withConstraints(profile)).asInstanceOf[T]

  protected def _withValidationProfile[T](profile: ValidationProfile, effectiveValidations: EffectiveValidations): T =
    copy(registry = registry.withConstraints(profile, effectiveValidations)).asInstanceOf[T]

  protected def _withTransformationPipeline[T](pipeline: TransformationPipeline): T =
    copy(registry = registry.withTransformationPipeline(pipeline)).asInstanceOf[T]

  protected def _withTransformationPipelines[T](pipelines: List[TransformationPipeline]): T =
    copy(registry = registry.withTransformationPipelines(pipelines)).asInstanceOf[T]

  protected def _withConstraintsRules[T](rules: Map[ProfileName, ValidationProfile]): T =
    copy(registry = registry.withConstraintsRules(rules)).asInstanceOf[T]

  protected def _withExecutionEnvironment[T](executionEnv: ExecutionEnvironment): T =
    copy(resolvers = resolvers.withExecutionEnvironment(executionEnv)).asInstanceOf[T]

  protected def copy(resolvers: AMFResolvers = resolvers,
                     errorHandlerProvider: ErrorHandlerProvider = errorHandlerProvider,
                     registry: AMFRegistry = registry,
                     listeners: Set[AMFEventListener] = Set.empty,
                     options: AMFOptions = options): BaseAMFConfigurationSetter

}
