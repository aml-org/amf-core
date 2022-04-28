package amf.core.internal.parser

import amf.core.client.common.remote.Content
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.ParserContext
import amf.core.internal.remote.{Cache, Context, Platform, Spec}
import amf.core.internal.utils.{AmfStrings, UriUtils}
import amf.core.internal.validation.core.ValidationSpecification
import org.mulesoft.common.client.lexical.SourceLocation

import scala.concurrent.{ExecutionContext, Future}

class CompilerContext(
    val url: String,
    val parserContext: ParserContext,
    val compilerConfig: CompilerConfiguration,
    val fileContext: Context,
    val allowedSpecs: Seq[Spec],
    cache: Cache
) {

  implicit val executionContext: ExecutionContext = compilerConfig.executionContext

  /** The resolved path that result to be the normalized url
    */
  val location: String = fileContext.current
  val path: String     = url.normalizePath

  def runInCache(fn: () => Future[BaseUnit]): Future[BaseUnit] = cache.getOrUpdate(location, fileContext)(fn)

  lazy val hasCycles: Boolean = fileContext.hasCycles

  lazy val platform: Platform = fileContext.platform

  def resolvePath(url: String): String = fileContext.resolve(UriUtils.normalizePath(url))

  def fetchContent(): Future[Content] = compilerConfig.resolveContent(location)

  def forReference(refUrl: String, allowedSpecs: Seq[Spec] = Seq.empty)(implicit
      executionContext: ExecutionContext
  ): CompilerContext = {

    new CompilerContextBuilder(refUrl, fileContext.platform, compilerConfig)
      .withFileContext(fileContext)
      .withBaseParserContext(parserContext)
      .withCache(cache)
      .withAllowedSpecs(allowedSpecs)
      .build()
  }

  def violation(id: ValidationSpecification, node: String, message: String, location: SourceLocation): Unit =
    compilerConfig.eh.violation(id, node, message, location)

  def violation(id: ValidationSpecification, message: String, location: SourceLocation): Unit =
    violation(id, "", message, location)
}
