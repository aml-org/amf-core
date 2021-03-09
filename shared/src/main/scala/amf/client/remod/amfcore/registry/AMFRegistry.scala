package amf.client.remod.amfcore.registry

import amf.ProfileName
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.resolution.{AMFResolutionPipeline, PipelineName}
import amf.core.validation.core.ValidationProfile

case class AMFRegistry(plugins: PluginsRegistry,
                       entitiesRegistry: EntitiesRegistry,
                       resolutionPipelines: Map[PipelineName, AMFResolutionPipeline],
                       constraintsRules: Map[ProfileName, ValidationProfile]) {

  def withPlugin(amfPlugin: AMFPlugin[_]): AMFRegistry = copy(plugins = plugins.withPlugin(amfPlugin))
  def withPlugins(amfPlugins: List[AMFPlugin[_]]): AMFRegistry = copy(plugins = plugins.withPlugins(amfPlugins))
}

object AMFRegistry{
  val empty = new AMFRegistry(PluginsRegistry.empty,EntitiesRegistry.empty, Map.empty, Map.empty)
}
