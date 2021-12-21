package amf.core.internal.parser

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.ParsingOptions
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.internal.rdf.SerializableAnnotationsFacade
import amf.core.internal.registries.{AMFRegistry, RegistryContext}

// configuration used by AMFParsePlugin
trait ParseConfiguration {
  def eh: AMFErrorHandler
  def sortedRootParsePlugins: Seq[AMFParsePlugin]
  def sortedReferenceParsePlugins: Seq[AMFParsePlugin]
  def parsingOptions: ParsingOptions
  def registryContext: RegistryContext
  def serializableAnnotationsFacade: SerializableAnnotationsFacade
}

case class ParseConfig(config: AMFGraphConfiguration, eh: AMFErrorHandler) extends ParseConfiguration {
  val sortedRootParsePlugins: Seq[AMFParsePlugin] =
    config.registry.getPluginsRegistry.rootParsePlugins.sorted
  val sortedReferenceParsePlugins: Seq[AMFParsePlugin] =
    config.registry.getPluginsRegistry.referenceParsePlugins.sorted
  val parsingOptions: ParsingOptions        = config.options.parsingOptions
  lazy val registryContext: RegistryContext = RegistryContext(config.getRegistry)
  lazy val serializableAnnotationsFacade    = new SerializableAnnotationsFacade(this)
}

object ParseConfig {

  /** use with caution, new error handler is created here */
  def apply(config: AMFGraphConfiguration): ParseConfiguration =
    ParseConfig(config, config.errorHandlerProvider.errorHandler())
}

/* Parse configuration that only contains error handler, all other content is left empty/default */
case class LimitedParseConfig(eh: AMFErrorHandler, registry: AMFRegistry = AMFRegistry.empty)
    extends ParseConfiguration {
  override def sortedRootParsePlugins: Seq[AMFParsePlugin]                  = Nil
  override def sortedReferenceParsePlugins: Seq[AMFParsePlugin]             = Nil
  override def parsingOptions: ParsingOptions                               = ParsingOptions()
  override def registryContext: RegistryContext                             = RegistryContext(registry)
  override def serializableAnnotationsFacade: SerializableAnnotationsFacade = new SerializableAnnotationsFacade(this)
}
