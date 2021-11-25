package amf.core.internal.parser

import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.{AMFEvent, UnitCache}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.parse.{AMFParsePlugin, AMFSyntaxParsePlugin}
import amf.core.internal.plugins.parse.DomainParsingFallback

import scala.concurrent.{ExecutionContext, Future}

/**
  * configuration used by AMFCompiler.
  */
case class CompilerConfiguration(private val config: AMFGraphConfiguration) {

  val eh: AMFErrorHandler = config.errorHandlerProvider.errorHandler()

  val executionContext: ExecutionContext           = config.resolvers.executionEnv.context
  def resolveContent(url: String): Future[Content] = config.resolvers.resolveContent(url)

  val sortedParseSyntax: Seq[AMFSyntaxParsePlugin] = config.registry.getPluginsRegistry.syntaxParsePlugins.sorted
  def notifyEvent(e: AMFEvent): Unit               = config.listeners.foreach(_.notifyEvent(e))

  def parsingFallback: DomainParsingFallback = config.registry.getPluginsRegistry.domainParsingFallback

  def getUnitsCache: Option[UnitCache] = config.getUnitsCache

  def generateParseConfiguration: ParseConfiguration = ParseConfig(config, eh)
}
