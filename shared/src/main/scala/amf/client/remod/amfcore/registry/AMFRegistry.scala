package amf.client.remod.amfcore.registry

import amf.ProfileName
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.resolution.PipelineInfo
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.client.remod.amfcore.plugins.parse.{AMFParsePlugin, DomainParsingFallback}
import amf.core.validation.core.ValidationProfile

private[amf] case class AMFRegistry(plugins: PluginsRegistry,
                                    entitiesRegistry: EntitiesRegistry,
                                    transformationPipelines: Map[String, ResolutionPipeline],
                                    constraintsRules: Map[ProfileName, ValidationProfile]) {

  def withPlugin(amfPlugin: AMFPlugin[_]): AMFRegistry = copy(plugins = plugins.withPlugin(amfPlugin))

  def removePlugin(id: String): AMFRegistry = copy(plugins = plugins.removePlugin(id))

  def withPlugins(amfPlugins: List[AMFPlugin[_]]): AMFRegistry = copy(plugins = plugins.withPlugins(amfPlugins))

  def withConstraints(profile: ValidationProfile): AMFRegistry =
    copy(constraintsRules = constraintsRules + (profile.name -> profile))

  def withTransformationPipeline(name: String, pipeline: ResolutionPipeline): AMFRegistry =
    copy(transformationPipelines = transformationPipelines + (name -> pipeline))

  def withTransformationPipelines(pipelines: Map[String, ResolutionPipeline]): AMFRegistry =
    copy(transformationPipelines = transformationPipelines ++ pipelines)

  private[amf] def getAllPlugins(): List[AMFPlugin[_]] = plugins.allPlugins

  private[amf] lazy val sortedParsePlugins: List[AMFParsePlugin] = plugins.parsePlugins.sorted

  private[amf] def getParsingFallback(): DomainParsingFallback = plugins.domainParsingFallback
}

object AMFRegistry {
  val empty = new AMFRegistry(PluginsRegistry.empty, EntitiesRegistry.empty, Map.empty, Map.empty)
}
