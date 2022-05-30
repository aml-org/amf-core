package amf.core.internal.parser

import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.{AMFEvent, UnitCache}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.parse.{AMFParsePlugin, AMFSyntaxParsePlugin}

import scala.concurrent.{ExecutionContext, Future}

/** configuration used by AMFCompiler.
  */
case class CompilerConfiguration(private val config: AMFGraphConfiguration) {

  val eh: AMFErrorHandler = config.errorHandlerProvider.errorHandler()

  val executionContext: ExecutionContext           = config.resolvers.executionEnv.context
  def resolveContent(url: String): Future[Content] = config.resolvers.resolveContent(url)

  val rootSortedParsePlugins: Seq[AMFParsePlugin] = config.registry.getPluginsRegistry.rootParsePlugins.sorted
  lazy val referenceSortedParsePlugins: Seq[AMFParsePlugin] =
    config.registry.getPluginsRegistry.referenceParsePlugins.sorted

  val sortedParseSyntax: Seq[AMFSyntaxParsePlugin] = config.registry.getPluginsRegistry.syntaxParsePlugins.sorted
  def notifyEvent(e: AMFEvent): Unit               = config.listeners.foreach(_.notifyEvent(e))

  def chooseFallback(document: Root, isRoot: Boolean): AMFParsePlugin = {
    val fallback = config.registry.getPluginsRegistry.domainParsingFallback
    val plugins  = if (isRoot) rootSortedParsePlugins else referenceSortedParsePlugins
    fallback.chooseFallback(document, plugins, isRoot)
  }

  def getUnitsCache: Option[UnitCache] = config.getUnitsCache

  def generateParseConfiguration: ParseConfiguration = ParseConfig(config, eh)
}
