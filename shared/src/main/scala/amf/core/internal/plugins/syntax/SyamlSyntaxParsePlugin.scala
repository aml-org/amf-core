package amf.core.internal.plugins.syntax

import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.parse.AMFSyntaxParsePlugin
import amf.core.client.scala.parse.document.{ParsedDocument, ParserContext, SyamlParsedDocument}
import amf.core.client.scala.validation.AMFValidationResult
import amf.core.internal.parser.domain.JsonParserFactory
import amf.core.internal.remote.Mimes
import amf.core.internal.remote.Mimes._
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.internal.validation.CoreValidations.SyamlError
import org.mulesoft.lexer.SourceLocation
import org.yaml.model._
import org.yaml.parser.YamlParser

import scala.collection.mutable

class SYamlAMFParserErrorHandler(eh: AMFErrorHandler) extends ParseErrorHandler with IllegalTypeHandler  {
  override def handle[T](error: YError, defaultValue: T): T = {
    eh.violation(SyamlError, "", error.error, part(error).location)
    defaultValue
  }

  final def handle(node: YPart, e: SyamlException): Unit = handle(node.location, e)

  override def handle(location: SourceLocation, e: SyamlException): Unit =
    eh.violation(SyamlError, "", e.getMessage, location)

  protected def part(error: YError): YPart = {
    error.node match {
      case d: YDocument => d
      case n: YNode     => n
      case s: YSuccess  => s.node
      case f: YFail     => part(f.error)
    }
  }
}

class SyamlAMFErrorHandler(val eh: AMFErrorHandler) extends AMFErrorHandler with ParseErrorHandler with IllegalTypeHandler  {
  val syamleh = new SYamlAMFParserErrorHandler(eh)

  override val results: mutable.LinkedHashSet[AMFValidationResult] = eh.results

  override def handle(location: SourceLocation, e: SyamlException): Unit = syamleh.handle(location, e)

  override def handle[T](error: YError, defaultValue: T): T = syamleh.handle(error, defaultValue)
}

object SyamlSyntaxParsePlugin extends AMFSyntaxParsePlugin with PlatformSecrets {

  private def getFormat(mediaType: String): String = if (mediaType.contains("json")) "json" else "yaml"

  override def parse(text: CharSequence, mediaType: String, ctx: ParserContext): ParsedDocument = {
    if (text.length() == 0) SyamlParsedDocument(YDocument(YNode.Null))
    else {
      val parser = getFormat(mediaType) match {
        case "json" => JsonParserFactory.fromCharsWithSource(text, ctx.rootContextDocument)(new SyamlAMFErrorHandler(ctx.eh))
        case _      => YamlParser(text, ctx.rootContextDocument)(new SyamlAMFErrorHandler(ctx.eh)).withIncludeTag("!include")
      }
      val document1 = parser.document()
      val (document, comment) = document1 match {
        case d if d.isNull =>
          (YDocument(Array(YNode(YMap.empty)), ctx.rootContextDocument), d.children collectFirst {
            case c: YComment => c.metaText
          })
        case d =>
          (d, d.children collectFirst { case c: YComment => c.metaText })
      }
      SyamlParsedDocument(document, comment)
    }
  }

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] =
    Seq(`application/yaml`,
        `application/x-yaml`,
        `text/yaml`,
        `text/x-yaml`,
        `application/json`,
        `text/json`,
        `text/vnd.yaml`)

  override def mainMediaType: String = `application/yaml`

  override val id: String = "syaml-parse"

  override def applies(element: CharSequence): Boolean = element.length() > 0

  override def priority: PluginPriority = NormalPriority
}
