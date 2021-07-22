package amf.core.internal.registries

import amf.core.client.scala.model.domain.{AnnotationGraphLoader, DomainElement}
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.common.validation.ProfileName
import amf.core.client.scala.render.AMFElementRenderPlugin
import amf.core.client.scala.transform.TransformationPipeline
import amf.core.internal.metamodel.ModelDefaultBuilder
import amf.core.internal.validation.core.ValidationProfile
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.plugins.parse.DomainParsingFallback
import amf.core.internal.registries.domain.EntitiesRegistry

/**
  * Registry to store plugins, entities, transformation pipelines and constraint rules
  *
  * @param plugins                 [[PluginsRegistry]]
  * @param entitiesRegistry        [[EntitiesRegistry]]
  * @param transformationPipelines a map of [[TransformationPipeline]]s
  * @param constraintsRules        a map of [[ProfileName]] -> [[amf.core.internal.validation.core.ValidationProfile]]
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

  def withConstraintsRules(rules: Map[ProfileName, ValidationProfile]): AMFRegistry =
    copy(constraintsRules = constraintsRules ++ rules)

  def withEntities(entities: Map[String, ModelDefaultBuilder]): AMFRegistry =
    copy(entitiesRegistry = entitiesRegistry.withEntities(entities))

  def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): AMFRegistry =
    copy(entitiesRegistry = entitiesRegistry.withAnnotations(annotations))

  def withExtensions(extensions: Seq[DomainElement]): AMFRegistry =
    copy(entitiesRegistry = entitiesRegistry.withExtensions(extensions))

  private[amf] def getAllPlugins(): List[AMFPlugin[_]] = plugins.allPlugins

  private[amf] lazy val sortedParsePlugins: List[AMFParsePlugin]                 = plugins.parsePlugins.sorted
  private[amf] lazy val sortedElementRenderPlugins: List[AMFElementRenderPlugin] = plugins.elementRenderPlugins.sorted

  private[amf] def getParsingFallback(): DomainParsingFallback = plugins.domainParsingFallback
}

object AMFRegistry {

  /** Creates an empty AMF Registry */
  val empty = new AMFRegistry(PluginsRegistry.empty, EntitiesRegistry.empty, Map.empty, Map.empty)
}
