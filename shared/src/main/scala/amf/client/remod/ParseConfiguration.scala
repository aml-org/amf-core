package amf.client.remod

import amf.client.remod.amfcore.config.{AMFEvent, ParsingOptions}
import amf.client.remod.amfcore.plugins.parse.{AMFParsePlugin, DomainParsingFallback}
import amf.client.remote.Content
import amf.core.Root
import amf.core.annotations.LexicalInformation
import amf.core.errorhandling.ErrorCollector
import amf.core.metamodel.ModelDefaultBuilder
import amf.core.model.document.BaseUnit
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, ParserContext}
import amf.core.parser.errorhandler.ParserErrorHandler
import amf.core.plugin.RegistryContext
import amf.core.rdf.helper.PluginEntitiesFacade
import amf.core.registries.AMFDomainRegistry.defaultIri
import amf.core.remote.PathResolutionError
import amf.core.utils.AmfStrings
import amf.internal.reference.UnitCache
import amf.plugins.features.validation.CoreValidations.UriSyntaxError

import java.net.URISyntaxException
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class ParseConfiguration(config: AMFGraphConfiguration, val url: String) {

  val eh: ParserErrorHandler = new ErrorCollector with ParserErrorHandler {
    private val errorHandler = config.errorHandlerProvider.errorHandler()
    override def reportConstraint(id: String,
                                  node: String,
                                  property: Option[String],
                                  message: String,
                                  lexical: Option[LexicalInformation],
                                  level: String,
                                  location: Option[String]): Unit =
      errorHandler.reportConstraint(id, node, property, message, lexical, level, location)
  }

  val executionContext: ExecutionContext           = config.resolvers.executionContext.executionContext
  def resolveContent(url: String): Future[Content] = config.resolvers.resolveContent(url)

  val sortedParsePlugins: immutable.Seq[AMFParsePlugin] = config.registry.plugins.parsePlugins.sorted
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

  def forUrl(url: String) = new ParseConfiguration(config, url)
}
