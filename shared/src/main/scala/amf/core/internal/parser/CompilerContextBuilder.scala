package amf.core.internal.parser

import amf.core.client.scala.parse.document.ParserContext
import amf.core.internal.remote.{Cache, Context, PathResolutionError, Platform, Spec}
import amf.core.internal.utils.AmfStrings
import amf.core.internal.validation.CoreValidations.UriSyntaxError

import java.net.URISyntaxException

class CompilerContextBuilder(url: String, platform: Platform, compilerConfig: CompilerConfiguration) {

  private var fileContext: Context                = Context(platform)
  private var cache                               = Cache()
  private var givenContent: Option[ParserContext] = None
  private var allowedSpecs: Seq[Spec]             = Nil

  def withFileContext(fc: Context): CompilerContextBuilder = {
    fileContext = fc
    this
  }

  def withCache(cache: Cache): CompilerContextBuilder = {
    this.cache = cache
    this
  }

  def withAllowedSpecs(allowed: Seq[Spec]): CompilerContextBuilder = {
    this.allowedSpecs = allowed
    this
  }

  def withBaseParserContext(parserContext: ParserContext): this.type = {
    givenContent = Some(parserContext)
    this
  }

  /**
    * normalized url
    * */
  private def path: String = {
    try {
      url.normalizePath
    } catch {
      case e: URISyntaxException =>
        compilerConfig.eh.violation(UriSyntaxError, url, e.getMessage)
        url
      case e: Exception => throw new PathResolutionError(e.getMessage)
    }
  }

  private def buildFileContext() = fileContext.update(path)

  private def buildParserContext(fc: Context) = givenContent match {
    case Some(given) => given.forLocation(fc.current)
    case None        => ParserContext(fc.current, config = compilerConfig.generateParseConfiguration)
  }

  def build(): CompilerContext = {
    val fc = buildFileContext()
    new CompilerContext(url, buildParserContext(fc), compilerConfig, fc, allowedSpecs, cache)
  }
}
