package amf.core.internal.render

import amf.core.client.scala.config._
import amf.core.client.scala.model.document.{BaseUnit, ExternalFragment}
import amf.core.client.scala.parse.document.{ParsedDocument, SyamlParsedDocument}
import amf.core.internal.plugins.render.{AMFGraphRenderPlugin, AMFRenderPlugin, RenderConfiguration, RenderInfo}
import amf.core.internal.remote.Mimes.`application/ld+json`
import amf.core.internal.remote.Platform
import amf.core.internal.unsafe.PlatformSecrets
import org.mulesoft.common.io.Output
import org.mulesoft.common.io.Output._
import org.yaml.builder.{JsonOutputBuilder, YDocumentBuilder}
import org.yaml.model.YDocument

import java.io.StringWriter
import scala.concurrent.{ExecutionContext, Future}

class AMFSerializer(unit: BaseUnit, config: RenderConfiguration, mediaType: Option[String]) extends PlatformSecrets {

  private val options = config.renderOptions

  case class RenderResult(document: ParsedDocument, syntax: String)

  def renderAsYDocument(renderPlugin: AMFRenderPlugin): RenderResult = {
    notifyEvent(StartingRenderingEvent(unit, renderPlugin, mediaType))
    val result = renderPlugin.emit(unit, config)
    notifyEvent(FinishedRenderingASTEvent(unit, result))
    RenderResult(result, mediaType.getOrElse(renderPlugin.defaultSyntax()))
  }

  private def notifyEvent(e: AMFEvent): Unit = config.listeners.foreach(_.notifyEvent(e))

  /** Print ast to writer. */
  def renderToWriter[W: Output](writer: W)(implicit executor: ExecutionContext): Unit = render(writer)

  /** Print ast to string. */
  def renderToString: String = render()

  /** Print ast to file. */
  def renderToFile(remote: Platform, path: String)(implicit executor: ExecutionContext): Future[Unit] =
    remote.write(path, renderToString)

  private def render[W: Output](writer: W): Unit = {
    notifyEvent(StartingRenderToWriterEvent(unit, mediaType))
    if (mediaType.contains(`application/ld+json`)) emitJsonldToWriter(writer)
    else {
      val renderPlugin = getRenderPlugin
      val renderResult = renderAsYDocument(renderPlugin)
      getSyntaxPlugin(renderResult) match {
        case Some(syntaxPlugin) =>
          syntaxPlugin.emit(renderResult.syntax, renderResult.document, writer)
          notifyEvent(FinishedRenderingSyntaxEvent(unit))
        case None if unit.isInstanceOf[ExternalFragment] =>
          writer.append(unit.asInstanceOf[ExternalFragment].encodes.raw.value())
        case _ => throw new Exception(s"Unsupported media type $mediaType")
      }
    }
  }

  def renderAST: ParsedDocument = {
    renderYDocumentWithPlugins.document
  }

  private[amf] def renderYDocumentWithPlugins: RenderResult = {
    val renderPlugin = getRenderPlugin
    renderAsYDocument(renderPlugin)
  }

  private def getSyntaxPlugin(renderResult: RenderResult) = {
    val candidates = config.syntaxPlugin.filter(_.mediaTypes.contains(renderResult.syntax))
    candidates.find(_.applies(renderResult.document))
  }

  private def emitJsonldToWriter[W: Output](writer: W): Unit = {
    val b = JsonOutputBuilder[W](writer, options.isPrettyPrint)
    AMFGraphRenderPlugin.emitToYDocBuilder(unit, b, config)
  }

  private[amf] def render(): String = {
    val w = new StringWriter
    render(w)
    w.toString
  }

  private[amf] def getRenderPlugin: AMFRenderPlugin = {
    val renderPlugin =
      config.renderPlugins
        .filter(p => p.mediaTypes.exists(mt => mediaType.forall(_ == mt)))
        .sorted
        .find(p => p.applies(RenderInfo(unit, mediaType.getOrElse(p.defaultSyntax()))))
    renderPlugin.getOrElse {
      throw new Exception(s"Cannot serialize domain model '${unit.location()}' for media type $mediaType")
    }
  }
}
