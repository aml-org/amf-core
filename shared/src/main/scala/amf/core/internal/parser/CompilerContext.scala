package amf.core.internal.parser

import amf.core.client.common.remote.Content
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.ParserContext
import amf.core.internal.remote.{Cache, Context, Platform}
import amf.core.internal.utils.AmfStrings
import amf.core.internal.validation.core.ValidationSpecification
import org.yaml.model.YPart

import scala.concurrent.{ExecutionContext, Future}

class CompilerContext(val url: String,
                      val parserContext: ParserContext,
                      val compilerConfig: CompilerConfiguration,
                      val fileContext: Context,
                      val allowedMediaTypes: Option[Seq[String]],
                      cache: Cache) {

  implicit val executionContext: ExecutionContext = compilerConfig.executionContext

  /**
    * The resolved path that result to be the normalized url
    */
  val location: String = fileContext.current
  val path: String     = url.normalizePath

  def runInCache(fn: () => Future[BaseUnit]): Future[BaseUnit] = cache.getOrUpdate(location, fileContext)(fn)

  lazy val hasCycles: Boolean = fileContext.hasCycles

  lazy val platform: Platform = fileContext.platform

  def resolvePath(url: String): String = fileContext.resolve(fileContext.platform.normalizePath(url))

  def fetchContent(): Future[Content] = compilerConfig.resolveContent(location)

  def forReference(refUrl: String, allowedMediaTypes: Option[Seq[String]] = None)(
      implicit executionContext: ExecutionContext): CompilerContext = {

    val builder = new CompilerContextBuilder(refUrl, fileContext.platform, compilerConfig)
      .withFileContext(fileContext)
      .withBaseParserContext(parserContext)
      .withCache(cache)

    allowedMediaTypes.foreach(builder.withAllowedMediaTypes)
    builder.build()
  }

  def violation(id: ValidationSpecification, node: String, message: String, ast: YPart): Unit =
    compilerConfig.eh.violation(id, node, message, ast)

  def violation(id: ValidationSpecification, message: String, ast: YPart): Unit = violation(id, "", message, ast)
}
