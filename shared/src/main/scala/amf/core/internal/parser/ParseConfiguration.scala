package amf.core.internal.parser

import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.{AMFEvent, ParsingOptions, UnitCache}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.internal.rdf.helper.{EntitiesFacade, SerializableAnnotationsFacade}
import amf.core.internal.registries.{AMFRegistry, RegistryContext}

import scala.collection.immutable

// configuration used by AMFParsePlugin
trait ParseConfiguration {
  def eh: AMFErrorHandler
  def sortedParsePlugins: immutable.Seq[AMFParsePlugin]
  def parsingOptions: ParsingOptions
  def registryContext: RegistryContext
  def entitiesFacade: EntitiesFacade
  def serializableAnnotationsFacade: SerializableAnnotationsFacade
}

case class ParseConfig(config: AMFGraphConfiguration, eh: AMFErrorHandler) extends ParseConfiguration {
  val sortedParsePlugins: immutable.Seq[AMFParsePlugin] = config.registry.plugins.parsePlugins.sorted
  val parsingOptions: ParsingOptions                    = config.options.parsingOptions
  lazy val registryContext: RegistryContext             = RegistryContext(config.getRegistry)
  lazy val entitiesFacade                               = new EntitiesFacade(this)
  lazy val serializableAnnotationsFacade                = new SerializableAnnotationsFacade(this)
}

object ParseConfig {

  /** use with caution, new error handler is created here */
  def apply(config: AMFGraphConfiguration): ParseConfiguration =
    ParseConfig(config, config.errorHandlerProvider.errorHandler())
}

/* Parse configuration that only contains error handler, all other content is left empty/default */
case class LimitedParseConfig(eh: AMFErrorHandler) extends ParseConfiguration {
  override def sortedParsePlugins: immutable.Seq[AMFParsePlugin]            = Nil
  override def parsingOptions: ParsingOptions                               = ParsingOptions()
  override def registryContext: RegistryContext                             = RegistryContext(AMFRegistry.empty)
  override def entitiesFacade: EntitiesFacade                               = new EntitiesFacade(this)
  override def serializableAnnotationsFacade: SerializableAnnotationsFacade = new SerializableAnnotationsFacade(this)
}
