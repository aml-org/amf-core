package amf.core

import amf.client.remod.amfcore.config.{
  AMFEvent,
  FinishedRenderingASTEvent,
  FinishedRenderingSyntaxEvent,
  StartingRenderingEvent
}
import amf.client.remod.amfcore.plugins.render.{
  AMFRenderPlugin,
  DefaultRenderConfiguration,
  RenderConfiguration,
  RenderInfo
}
import amf.core.benchmark.ExecutionLog
import amf.core.emitter.{RenderOptions, ShapeRenderOptions}
import amf.core.model.document.{BaseUnit, ExternalFragment}
import amf.core.parser.SyamlParsedDocument
import amf.core.rdf.RdfModelDocument
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote.{MediaTypeParser, Platform, Vendor}
import amf.core.unsafe.PlatformSecrets
import amf.core.vocabulary.{Namespace, NamespaceAliases}
import amf.plugins.document.graph._
import amf.plugins.document.graph.emitter.{EmbeddedJsonLdEmitter, FlattenedJsonLdEmitter}
import amf.plugins.syntax.RdfSyntaxPlugin
import org.mulesoft.common.io.Output
import org.mulesoft.common.io.Output._
import org.yaml.builder.{DocBuilder, JsonOutputBuilder, YDocumentBuilder}
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

  /** Render to doc builder. */
  def renderToBuilder[T](builder: DocBuilder[T])(implicit executor: ExecutionContext): Unit =
    mediaTypeExp.getPureVendorExp match {
      case Vendor.AMF.mediaType =>
        val namespaceAliases = generateNamespaceAliasesFromPlugins
        config.renderOptions.toGraphSerialization match {
          case JsonLdSerialization(FlattenedForm) =>
            FlattenedJsonLdEmitter.emit(unit, builder, options, namespaceAliases)
          case JsonLdSerialization(EmbeddedForm) =>
            EmbeddedJsonLdEmitter.emit(unit, builder, options, namespaceAliases)
        }
    }

  private def generateNamespaceAliasesFromPlugins: NamespaceAliases =
    config.namespacePlugins.sorted
      .find(_.applies(unit))
      .map(_.aliases(unit))
      .getOrElse(Namespace.defaultAliases)

  /** Print ast to writer. */
  def renderToWriter[W: Output](writer: W)(implicit executor: ExecutionContext): Unit = render(writer)

  /** Print ast to string. */
  def renderToString: String = render()

  /** Print ast to file. */
  def renderToFile(remote: Platform, path: String)(implicit executor: ExecutionContext): Future[Unit] =
    remote.write(path, renderToString)

  private def render[W: Output](writer: W): Unit = {
    ExecutionLog.log(s"AMFSerializer#render: Rendering to $mediaType ($mediaType file) ${unit.location()}")
    mediaTypeExp.getPureVendorExp match {
      case Vendor.AMF.mediaType =>
        config.renderOptions.toGraphSerialization match {
          case RdfSerialization()                => emitRdf(writer)
          case JsonLdSerialization(documentForm) => emitJsonLd(writer, documentForm)
        }
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

  private def getSyntaxPlugin(ast: SyamlParsedDocument, mediaType: String) = {
    val candidates = config.syntaxPlugin.filter(_.mediaTypes.contains(mediaType))
    candidates.find(_.applies(ast))
  }

  private def emitJsonLd[W: Output](writer: W, form: JsonLdDocumentForm): Unit = {
    val b                = JsonOutputBuilder[W](writer, options.isPrettyPrint)
    val namespaceAliases = generateNamespaceAliasesFromPlugins
    form match {
      case FlattenedForm => FlattenedJsonLdEmitter.emit(unit, b, options, namespaceAliases)
      case EmbeddedForm  => EmbeddedJsonLdEmitter.emit(unit, b, options, namespaceAliases)
      case _             => // Ignore
    }
  }

  private def emitRdf[W: Output](writer: W): Unit =
    platform.rdfFramework match {
      case Some(r) =>
        val d = RdfModelDocument(r.unitToRdfModel(unit, config.renderOptions))
        RdfSyntaxPlugin.unparse(mediaType, d, writer)
      case _ => None
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

object AMFSerializer {

  private def generateRenderEnv(options: Option[RenderOptions],
                                shapeOptions: ShapeRenderOptions): RenderConfiguration = {
    val renderOptions    = options.getOrElse(RenderOptions())
    val immutableOptions = RenderOptions.toImmutable(renderOptions, ShapeRenderOptions.toImmutable(shapeOptions))

    val env = AMFPluginsRegistry
      .obtainStaticConfig()
      .withRenderOptions(immutableOptions)
    DefaultRenderConfiguration(env)
  }
}
