package amf.client.remod

import amf.client.remod.amfcore.config.{AMFEvent, ParsingOptions}
import amf.client.remod.amfcore.plugins.parse.{AMFParsePlugin, AMFSyntaxPlugin, DomainParsingFallback}
import amf.client.remote.Content
import amf.core.Root
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.plugin.RegistryContext
import amf.core.rdf.helper.{EntitiesFacade, SerializableAnnotationsFacade}
import amf.internal.reference.UnitCache

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

case class ParseConfiguration private (config: AMFGraphConfiguration, eh: AMFErrorHandler) {

  val executionContext: ExecutionContext           = config.resolvers.executionContext.executionContext
  def resolveContent(url: String): Future[Content] = config.resolvers.resolveContent(url)

  val sortedParsePlugins: immutable.Seq[AMFParsePlugin] = config.registry.plugins.parsePlugins.sorted
  val sortedParseSyntax: immutable.Seq[AMFSyntaxPlugin] = config.registry.plugins.syntaxPlugin.sorted
  val domainFallback: DomainParsingFallback             = config.registry.plugins.domainParsingFallback
  val parsingOptions: ParsingOptions                    = config.options.parsingOptions
  def notifyEvent(e: AMFEvent): Unit                    = config.listeners.foreach(_.notifyEvent(e))

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
