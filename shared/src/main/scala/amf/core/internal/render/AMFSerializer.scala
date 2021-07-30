package amf.core.internal.render

import amf.core.client.scala.config._
import amf.core.client.scala.model.document.{BaseUnit, ExternalFragment}
import amf.core.client.scala.parse.document.{ParsedDocument, SyamlParsedDocument}
import amf.core.internal.plugins.render.{AMFGraphRenderPlugin, AMFRenderPlugin, RenderConfiguration, RenderInfo}
import amf.core.internal.remote.{MediaTypeParser, Platform, Vendor}
import amf.core.internal.unsafe.PlatformSecrets
import org.mulesoft.common.io.Output
import org.mulesoft.common.io.Output._
import org.yaml.builder.{JsonOutputBuilder, YDocumentBuilder}
import org.yaml.model.YDocument

import java.io.StringWriter
import scala.concurrent.{ExecutionContext, Future}

class AMFSerializer(unit: BaseUnit, mediaType: String, config: RenderConfiguration) extends PlatformSecrets {

  private val mediaTypeExp = new MediaTypeParser(mediaType)

  private val options = config.renderOptions

  def renderAsYDocument(renderPlugin: AMFRenderPlugin): SyamlParsedDocument = {

    val builder = new YDocumentBuilder
    notifyEvent(StartingRenderingEvent(unit, renderPlugin, mediaType))
    if (renderPlugin.emit(unit, builder, config)) {
      val result = SyamlParsedDocument(builder.result.asInstanceOf[YDocument])
      notifyEvent(FinishedRenderingASTEvent(unit, result))
      result
    } else throw new Exception(s"Error unparsing syntax $mediaType with domain plugin ${renderPlugin.id}")
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
    mediaTypeExp.getPureVendorExp match {
      case Vendor.AMF.mediaType => emitJsonldToWriter(writer)
      case _ =>
        val renderPlugin = getRenderPlugin
        val ast          = renderAsYDocument(renderPlugin)
        val mT           = mediaTypeExp.getSyntaxExp.getOrElse(renderPlugin.defaultSyntax())
        getSyntaxPlugin(ast, mT) match {
          case Some(syntaxPlugin) =>
            syntaxPlugin.emit(mT, ast, writer)
            notifyEvent(FinishedRenderingSyntaxEvent(unit))
          case None if unit.isInstanceOf[ExternalFragment] =>
            writer.append(unit.asInstanceOf[ExternalFragment].encodes.raw.value())
          case _ => throw new Exception(s"Unsupported media type $mediaType")
        }
    }
  }

  def renderAST: ParsedDocument = {
    mediaTypeExp.getPureVendorExp match {
      case _ => renderYDocumentWithPlugins
    }
  }

  private[amf] def renderYDocumentWithPlugins: SyamlParsedDocument = {
    val renderPlugin = getRenderPlugin
    renderAsYDocument(renderPlugin)
  }

  private def getSyntaxPlugin(ast: SyamlParsedDocument, mediaType: String) = {
    val candidates = config.syntaxPlugin.filter(_.mediaTypes.contains(mediaType))
    candidates.find(_.applies(ast))
  }

  private def emitJsonldToWriter[W: Output](writer: W): Unit = {
    val b = JsonOutputBuilder[W](writer, options.isPrettyPrint)
    AMFGraphRenderPlugin.emit(unit, b, config)
  }

  private[amf] def render(): String = {
    val w = new StringWriter
    render(w)
    w.toString
  }

  private[amf] def getRenderPlugin: AMFRenderPlugin = {
    val renderPlugin =
      config.renderPlugins.filter(_.mediaTypes.contains(mediaType)).sorted.find(_.applies(RenderInfo(unit, mediaType)))
    renderPlugin.getOrElse {
      throw new Exception(s"Cannot serialize domain model '${unit.location()}' for media type $mediaType")
    }
  }
}
