package amf.client.remod

import amf.client.remod.amfcore.config.{AMFConfig, AMFOptions, AMFResolvers}
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.plugins.parse.AMFParsePlugin
import amf.client.remod.amfcore.registry.AMFRegistry
import amf.core.client.ParsingOptions
import amf.core.validation.core.ValidationProfile
import amf.internal.environment.Environment
import amf.internal.reference.UnitCache
import amf.internal.resource.ResourceLoader
// all constructors only visible from amf. Users should always use builders or defaults

abstract private[amf] class BaseEnvironment(val resolvers: AMFResolvers,
                                            val errorHandlerProvider: ErrorHandlerProvider,
                                            val registry: AMFRegistry,
                                            val amfConfig: AMFConfig,
                                            val options: AMFOptions){
  type Self <: BaseEnvironment
//  private var initialized:Boolean = false

//  def init(): Future[Unit] = if(initialized) Future.unit else registry.init().map(_ => initialized = true)

  def getInstance():AMFClient = new AMFClient(this)

  def withResourceLoader(rl:ResourceLoader): Self = doCopy(resolvers.withResourceLoader(rl))

  def overrideResourceLoaders(rl:Seq[ResourceLoader]): Self = doCopy(AMFResolvers(rl, resolvers.unitCache))

  def withUnitCache(cache: UnitCache): Self = doCopy(AMFResolvers(resolvers.resourceLoaders, Some(cache)))

  def withPlugin(amfPlugin: AMFParsePlugin): Self = doCopy(registry.withPlugin(amfPlugin))

  def withPlugins(plugins: List[AMFPlugin[_]]): Self = doCopy(registry.withPlugins(plugins))

  def withConstraints(profile:ValidationProfile) : Self = doCopy(registry.withConstraints(profile))

//  private [amf] def beforeParse() = init()

  def withoutPlugin(plugin: AMFPlugin[_]): Self = doCopy(registry.withoutPlugin(plugin))

  // Can uses Lenses for this! Lenses are used to simplify copying nested data structures
  protected def doCopy(registry: AMFRegistry): Self

  protected def doCopy(resolvers: AMFResolvers): Self
}

object BaseEnvironment {
  def fromLegacy(base: BaseEnvironment, legacy: Environment): BaseEnvironment = {
    legacy.maxYamlReferences.foreach{ maxValue => base.options.parsingOptions.setMaxYamlReferences(maxValue) }
    val withLoaders = base.overrideResourceLoaders(legacy.loaders)
    legacy.resolver.map( unitCache => withLoaders.withUnitCache(unitCache)).getOrElse(withLoaders)
  }
}

case class AMFEnvironment(override val resolvers: AMFResolvers,
                          override val errorHandlerProvider: ErrorHandlerProvider,
                          override val registry: AMFRegistry,
                          override val amfConfig: AMFConfig,
                          override val options: AMFOptions) extends BaseEnvironment(resolvers, errorHandlerProvider, registry, amfConfig, options) { // break platform into more specific classes?
  type Self = AMFEnvironment

  def withParsingOptions(parsingOptions: ParsingOptions): AMFEnvironment = this.copy(options = options.copy(parsingOptions = parsingOptions))

  override protected def doCopy(registry: AMFRegistry): Self = this.copy(registry = registry)

  override protected def doCopy(resolvers: AMFResolvers): Self = this.copy(resolvers = resolvers)

  /**
    * Merges two environments taking into account specific attributes that can be merged.
    * This is currently limited to: registry plugins.
    */
  def merge(other: AMFEnvironment): AMFEnvironment = {
    this.withPlugins(other.registry.plugins.allPlugins)
  }

}

object AMFEnvironment {

  def predefined(): AMFEnvironment = {
    val config = AMFConfig.predefined()
    new AMFEnvironment(
      AMFResolvers(config.platform.loaders()(config.executionContext.context),None),
      DefaultErrorHandlerProvider,
      AMFRegistry.empty,
      config,
      AMFOptions.default()
    )
  }
}

