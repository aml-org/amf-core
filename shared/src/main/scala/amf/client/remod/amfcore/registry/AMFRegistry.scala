package amf.client.remod.amfcore.registry

import amf.ProfileName
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.plugins.parse.{AMFParsePlugin, DomainParsingFallback}
import amf.core.metamodel.{ModelDefaultBuilder, Obj}
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.validation.core.ValidationProfile

/**
  * Registry to store plugins, entities, transformation pipelines and constraint rules
  * @param plugins                 [[amf.client.remod.amfcore.registry.PluginsRegistry]]
  * @param entitiesRegistry        [[amf.client.remod.amfcore.registry.EntitiesRegistry]]
  * @param transformationPipelines a map of [[amf.core.resolution.pipelines.TransformationPipeline]]s
  * @param constraintsRules        a map of [[amf.ProfileName]] -> [[amf.core.validation.core.ValidationProfile]]
  */
private[amf] case class AMFRegistry(plugins: PluginsRegistry,
                                    entitiesRegistry: EntitiesRegistry,
                                    transformationPipelines: Map[String, TransformationPipeline],
                                    constraintsRules: Map[ProfileName, ValidationProfile]) {

  def withPlugin(amfPlugin: AMFPlugin[_]): AMFRegistry = copy(plugins = plugins.withPlugin(amfPlugin))

  def removePlugin(id: String): AMFRegistry = copy(plugins = plugins.removePlugin(id))

  def withPlugins(amfPlugins: List[AMFPlugin[_]]): AMFRegistry = copy(plugins = plugins.withPlugins(amfPlugins))

  def withConstraints(profile: ValidationProfile): AMFRegistry =
    copy(constraintsRules = constraintsRules + (profile.name -> profile))

  def removeConstraints(name: ProfileName): AMFRegistry =
    copy(constraintsRules = constraintsRules - name)

  def withTransformationPipeline(pipeline: TransformationPipeline): AMFRegistry =
    copy(transformationPipelines = transformationPipelines + (pipeline.name -> pipeline))

  def withTransformationPipelines(pipelines: List[TransformationPipeline]): AMFRegistry =
    copy(transformationPipelines = transformationPipelines ++ pipelines.map(p => p.name -> p))

  def withEntities(entities: Map[String, ModelDefaultBuilder]): AMFRegistry =
    copy(entitiesRegistry = entitiesRegistry.withEntities(entities))

  def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): AMFRegistry =
    copy(entitiesRegistry = entitiesRegistry.withAnnotations(annotations))

  private[amf] def getAllPlugins(): List[AMFPlugin[_]] = plugins.allPlugins

  private[amf] lazy val sortedParsePlugins: List[AMFParsePlugin] = plugins.parsePlugins.sorted

  private[amf] def getParsingFallback(): DomainParsingFallback = plugins.domainParsingFallback
}

object AMFRegistry {

  /** Creates an empty AMF Registry */
  val empty = new AMFRegistry(PluginsRegistry.empty, EntitiesRegistry.empty, Map.empty, Map.empty)
}
