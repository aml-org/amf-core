package amf.core.internal.parser

import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.{AMFEvent, ParsingOptions, UnitCache}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.{AMFParsePlugin, AMFSyntaxParsePlugin}
import amf.core.internal.plugins.parse.DomainParsingFallback
import amf.core.internal.rdf.helper.{EntitiesFacade, SerializableAnnotationsFacade}
import amf.core.internal.registries.RegistryContext

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

case class ParseConfiguration private (config: AMFGraphConfiguration, eh: AMFErrorHandler) {

  val executionContext: ExecutionContext           = config.resolvers.executionEnv.context
  def resolveContent(url: String): Future[Content] = config.resolvers.resolveContent(url)

  val sortedParsePlugins: immutable.Seq[AMFParsePlugin]      = config.registry.plugins.parsePlugins.sorted
  val sortedParseSyntax: immutable.Seq[AMFSyntaxParsePlugin] = config.registry.plugins.syntaxParsePlugins.sorted
  val domainFallback: DomainParsingFallback                  = config.registry.plugins.domainParsingFallback
  val parsingOptions: ParsingOptions                         = config.options.parsingOptions
  def notifyEvent(e: AMFEvent): Unit                         = config.listeners.foreach(_.notifyEvent(e))

  def chooseFallback(document: Root, mediaType: Option[String]): BaseUnit =
    domainFallback.chooseFallback(document, mediaType, sortedParsePlugins)

  def getUnitsCache: Option[UnitCache] = config.getUnitsCache

  private[amf] lazy val registryContext: RegistryContext = RegistryContext(config.getRegistry)

  lazy val entitiesFacade                = new EntitiesFacade(this)
  lazy val serializableAnnotationsFacade = new SerializableAnnotationsFacade(this)

}

object ParseConfiguration {

  /** use with caution, new error handler is created here */
  def apply(config: AMFGraphConfiguration): ParseConfiguration =
    ParseConfiguration(config, config.errorHandlerProvider.errorHandler())
  def apply(eh: AMFErrorHandler): ParseConfiguration =
    ParseConfiguration(AMFGraphConfiguration.predefined(), eh)
}
