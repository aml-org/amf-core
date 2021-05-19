package amf.client.remod

import amf.client.remod.amfcore.config.{AMFEvent, ParsingOptions}
import amf.client.remod.amfcore.plugins.parse.{AMFParsePlugin, AMFSyntaxPlugin, DomainParsingFallback}
import amf.client.remote.Content
import amf.core.Root
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.parser.ParserContext
import amf.core.plugin.RegistryContext
import amf.core.rdf.helper.PluginEntitiesFacade
import amf.core.remote.PathResolutionError
import amf.core.utils.AmfStrings
import amf.internal.reference.UnitCache
import amf.plugins.features.validation.CoreValidations.UriSyntaxError

import java.net.URISyntaxException
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

case class ParseConfiguration private (config: AMFGraphConfiguration, url: String, eh: AMFErrorHandler) {

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

  /**
    * normalized url
    * */
  val path: String = {
    try {
      url.normalizePath
    } catch {
      case e: URISyntaxException =>
        eh.violation(UriSyntaxError, url, e.getMessage)
        url
      case e: Exception => throw new PathResolutionError(e.getMessage)
    }
  }
  val parserContext: ParserContext                       = ParserContext(path, eh = eh)
  private[amf] lazy val registryContext: RegistryContext = RegistryContext(config.getRegistry)

  lazy val entitiesFacade = new PluginEntitiesFacade(this)

  def forUrl(url: String) = new ParseConfiguration(config, url, eh)
}

object ParseConfiguration {

  /** use with caution, new error handler is created here */
  def apply(config: AMFGraphConfiguration, url: String): ParseConfiguration =
    ParseConfiguration(config, url, config.errorHandlerProvider.errorHandler())
}
