package amf.core.client.scala.parse

import amf.core.client.scala.resource.ResourceLoader
import amf.core.client.scala.{AMFGraphConfiguration, AMFObjectResult, AMFParseResult, AMFResult}
import amf.core.internal.convert.CoreClientConverters.platform
import amf.core.internal.parser.{AMFCompiler, AMFGraphPartialCompiler, AmfObjectUnitContainer, CompilerConfiguration, CompilerContextBuilder}
import amf.core.internal.remote.{Cache, Context}
import amf.core.internal.resource.StringResourceLoader

import scala.concurrent.{ExecutionContext, Future}

object AMFParser {

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url Location of the api
    * @param configuration [[AMFGraphConfiguration]]
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parse(url: String, configuration: AMFGraphConfiguration): Future[AMFParseResult] =
    parseAsync(url, configuration)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The unit to parse as a string
    * @param configuration [[AMFGraphConfiguration]]
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parseContent(content: String, configuration: AMFGraphConfiguration): Future[AMFParseResult] = {
    val preferredSyntaxPlugin = configuration.registry.getPluginsRegistry.syntaxParsePlugins.sorted.headOption
    val defaultMediaType      = preferredSyntaxPlugin.map(_.mainMediaType)
    parseContent(content, DEFAULT_DOCUMENT_URL, defaultMediaType, configuration)
  }

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The unit as a string
    * @param mediaType The nature and format of the given content e.g. <code>application/yaml</code> or <code>application/json</code>
    * @param configuration [[AMFGraphConfiguration]]
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parseContent(content: String, mediaType: String, configuration: AMFGraphConfiguration): Future[AMFParseResult] =
    parseContent(content, DEFAULT_DOCUMENT_URL, Some(mediaType), configuration)

  def parseStartingPoint(graphUrl: String,
                         startingPoint: String,
                         configuration: AMFGraphConfiguration): Future[AMFObjectResult] = {
    val compilerConfiguration                       = configuration.compilerConfiguration
    implicit val executionContext: ExecutionContext = compilerConfiguration.executionContext
    val context = new CompilerContextBuilder(graphUrl, platform, compilerConfiguration)
      .withCache(Cache())
      .withFileContext(Context(platform))
      .build()
    val compiler = new AMFGraphPartialCompiler(context, startingPoint)
    build(compiler, compilerConfiguration).map { r =>
      r.baseUnit match {
        case container: AmfObjectUnitContainer => new AMFObjectResult(container.result, r.results)
        case _                                 => throw new UnsupportedOperationException("Unexpected result unit type for partial parsing")
      }
    }
  }

  private[amf] def parseContent(content: String,
                                url: String,
                                mediaType: Option[String],
                                configuration: AMFGraphConfiguration): Future[AMFParseResult] = {
    val loader     = fromStream(url, content, mediaType)
    val withLoader = configuration.withResourceLoader(loader)
    parseAsync(url, withLoader)
  }

  private[amf] def parseAsync(url: String, amfConfig: AMFGraphConfiguration): Future[AMFParseResult] = {
    val compilerConfig                              = amfConfig.compilerConfiguration
    implicit val executionContext: ExecutionContext = compilerConfig.executionContext
    build(AMFCompiler(url, Context(platform), Cache(), compilerConfig), compilerConfig)
  }

  private def build(compiler: AMFCompiler, compilerConfig: CompilerConfiguration)(implicit context: ExecutionContext) = {
    compiler
      .build()
      .map { model =>
        val results = compilerConfig.eh.getResults
        new AMFParseResult(model, results)
      }
  }

  private def fromStream(url: String, stream: String, mediaType: Option[String]): ResourceLoader =
    StringResourceLoader(platform.resolvePath(url), stream, mediaType)

  private[amf] val DEFAULT_DOCUMENT_URL = "http://a.ml/amf/default_document"
}
