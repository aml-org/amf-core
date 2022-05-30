package amf.core.internal.registries

import amf.core.client.common.validation.ProfileName
import amf.core.client.scala.model.domain.AnnotationGraphLoader
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.render.AMFElementRenderPlugin
import amf.core.client.scala.transform.TransformationPipeline
import amf.core.client.scala.validation.EffectiveValidationsCompute
import amf.core.client.scala.vocabulary.NamespaceAliases
import amf.core.internal.metamodel.ModelDefaultBuilder
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.plugins.parse.DomainParsingFallback
import amf.core.internal.registries.domain.EntitiesRegistry
import amf.core.internal.validation.core.ValidationProfile
import amf.core.internal.validation.EffectiveValidations

/** Registry to store plugins, entities, transformation pipelines and constraint rules
  *
  * @param plugins
  *   [[PluginsRegistry]]
  * @param entitiesRegistry
  *   [[EntitiesRegistry]]
  * @param transformationPipelines
  *   a map of [[TransformationPipeline]]s
  * @param constraintsRules
  *   a map of [[ProfileName]] -> [[amf.core.internal.validation.core.ValidationProfile]]
  */
private[amf] class AMFRegistry(
    plugins: PluginsRegistry,
    entitiesRegistry: EntitiesRegistry,
    transformationPipelines: Map[String, TransformationPipeline],
    constraintsRules: Map[ProfileName, ValidationProfile],
    effectiveValidations: Map[ProfileName, EffectiveValidations],
    namespaceAliases: NamespaceAliases
) {

  def getPluginsRegistry: PluginsRegistry                             = plugins
  def getEntitiesRegistry: EntitiesRegistry                           = entitiesRegistry
  def getTransformationPipelines: Map[String, TransformationPipeline] = transformationPipelines
  def getConstraintsRules: Map[ProfileName, ValidationProfile]        = constraintsRules
  def getEffectiveValidations: Map[ProfileName, EffectiveValidations] = effectiveValidations
  def getNamespaceAliases: NamespaceAliases                           = namespaceAliases

  def withPlugin(amfPlugin: AMFPlugin[_]): AMFRegistry = copy(plugins = plugins.withPlugin(amfPlugin))

  def withReferenceParsePlugin(parsePlugin: AMFParsePlugin): AMFRegistry =
    copy(plugins = plugins.withReferenceParsePlugin(parsePlugin))

  def withPlugins(amfPlugins: List[AMFPlugin[_]]): AMFRegistry = copy(plugins = plugins.withPlugins(amfPlugins))

  def withFallback(plugin: DomainParsingFallback): AMFRegistry = copy(plugins = plugins.withFallback(plugin))

  def withConstraints(profile: ValidationProfile): AMFRegistry = {
    val nextRules         = constraintsRules + (profile.name -> profile)
    val computedEffective = EffectiveValidationsCompute.buildAll(nextRules)
    copy(constraintsRules = nextRules, effectiveValidations = computedEffective)
  }

  def withConstraints(profile: ValidationProfile, effective: EffectiveValidations): AMFRegistry = {
    val nextRules     = constraintsRules + (profile.name     -> profile)
    val nextEffective = effectiveValidations + (profile.name -> effective)
    copy(constraintsRules = nextRules, effectiveValidations = nextEffective)
  }

  def withConstraintsRules(rules: Map[ProfileName, ValidationProfile]): AMFRegistry = {
    val nextRules         = constraintsRules ++ rules
    val computedEffective = EffectiveValidationsCompute.buildAll(nextRules)
    copy(constraintsRules = nextRules, effectiveValidations = computedEffective)
  }

  def withTransformationPipeline(pipeline: TransformationPipeline): AMFRegistry =
    copy(transformationPipelines = transformationPipelines + (pipeline.name -> pipeline))

  def withTransformationPipelines(pipelines: List[TransformationPipeline]): AMFRegistry =
    copy(transformationPipelines = transformationPipelines ++ pipelines.map(p => p.name -> p))

  def withEntities(entities: Map[String, ModelDefaultBuilder]): AMFRegistry =
    copy(entitiesRegistry = entitiesRegistry.withEntities(entities))

  def emptyEntities(): AMFRegistry = copy(entitiesRegistry = entitiesRegistry.removeAllEntities())

  def emptyPlugins(): AMFRegistry = copy(plugins = plugins.removeAllPlugins())

  def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): AMFRegistry =
    copy(entitiesRegistry = entitiesRegistry.withAnnotations(annotations))

  def withAliases(aliases: NamespaceAliases): AMFRegistry =
    copy(namespaceAliases = namespaceAliases.merge(aliases))

  private def copy(
      plugins: PluginsRegistry = plugins,
      entitiesRegistry: EntitiesRegistry = entitiesRegistry,
      transformationPipelines: Map[String, TransformationPipeline] = transformationPipelines,
      constraintsRules: Map[ProfileName, ValidationProfile] = constraintsRules,
      effectiveValidations: Map[ProfileName, EffectiveValidations] = effectiveValidations,
      namespaceAliases: NamespaceAliases = namespaceAliases
  ): AMFRegistry =
    new AMFRegistry(
        plugins,
        entitiesRegistry,
        transformationPipelines,
        constraintsRules,
        effectiveValidations,
        namespaceAliases
    )

  private[amf] lazy val sortedElementRenderPlugins: List[AMFElementRenderPlugin] = plugins.elementRenderPlugins.sorted

  private[amf] def getParsingFallback(): DomainParsingFallback = plugins.domainParsingFallback
}

object AMFRegistry {

  /** Creates an empty AMF Registry */
  val empty =
    new AMFRegistry(PluginsRegistry.empty, EntitiesRegistry.empty, Map.empty, Map.empty, Map.empty, NamespaceAliases())
}
