package amf.core

import amf.client.exported.config.AMFEvent
import amf.client.remod.amfcore.config.{
  FinishedRenderingASTEvent,
  FinishedRenderingSyntaxEvent,
  StartingRenderingEvent
}

import java.io.StringWriter
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
import amf.core.remote.{Platform, Vendor}
import amf.core.services.RuntimeSerializer
import amf.core.vocabulary.Namespace
import amf.plugins.document.graph.AMFGraphPlugin.platform
import amf.plugins.document.graph.{
  EmbeddedForm,
  FlattenedForm,
  JsonLdDocumentForm,
  JsonLdSerialization,
  RdfSerialization
}
import amf.plugins.document.graph.emitter.{EmbeddedJsonLdEmitter, FlattenedJsonLdEmitter}
import amf.plugins.syntax.RdfSyntaxPlugin
import org.mulesoft.common.io.Output
import org.mulesoft.common.io.Output._
import org.yaml.builder.{DocBuilder, JsonOutputBuilder, YDocumentBuilder}
import org.yaml.model.YDocument

import scala.concurrent.{ExecutionContext, Future}

class AMFSerializer(unit: BaseUnit, mediaType: String, vendor: String, config: RenderConfiguration) {

  private def this(unit: BaseUnit,
                   mediaType: String,
                   vendor: String,
                   options: Option[RenderOptions],
                   shapeOptions: ShapeRenderOptions) =
    this(unit, mediaType, vendor, AMFSerializer.generateRenderEnv(options, shapeOptions))

  private def this(unit: BaseUnit, mediaType: String, vendor: String, shapeOptions: ShapeRenderOptions) =
    this(unit, mediaType, vendor, None, shapeOptions)

  // maintained for compatibility reasons
  def this(unit: BaseUnit,
           mediaType: String,
           vendor: String,
           options: RenderOptions,
           shapeOptions: ShapeRenderOptions = ShapeRenderOptions()) =
    this(unit, mediaType, vendor, Some(options), shapeOptions)

  private val options       = config.renderOptions
  private val legacyOptions = RenderOptions.fromImmutable(options, config.errorHandler)

  def renderAsYDocument(): SyamlParsedDocument = {
    val renderPlugin = getRenderPlugin
    val builder      = new YDocumentBuilder
    notifyEvent(StartingRenderingEvent(unit, renderPlugin, mediaType))
    if (renderPlugin.emit(unit, builder, options, config.errorHandler)) {
      val result = SyamlParsedDocument(builder.result.asInstanceOf[YDocument])
      notifyEvent(FinishedRenderingASTEvent(unit, result))
      result
    } else throw new Exception(s"Error unparsing syntax $mediaType with domain plugin ${renderPlugin.id}")
  }

  private def notifyEvent(e: AMFEvent): Unit = config.listeners.foreach(_.notifyEvent(e))

  /** Render to doc builder. */
  def renderToBuilder[T](builder: DocBuilder[T])(implicit executor: ExecutionContext): Future[Unit] = Future {
    vendor match {
      case Vendor.AMF.name =>
        val namespaceAliases = generateNamespaceAliasesFromPlugins
        legacyOptions.toGraphSerialization match {
          case JsonLdSerialization(FlattenedForm) =>
            FlattenedJsonLdEmitter.emit(unit, builder, options, namespaceAliases)
          case JsonLdSerialization(EmbeddedForm) =>
            EmbeddedJsonLdEmitter.emit(unit, builder, options, namespaceAliases)
        }
    }
  }

  private def generateNamespaceAliasesFromPlugins[T] = {
    AMFPluginsRegistry.documentPlugins
      .find(_.canGenerateNamespaceAliases(unit))
      .map(_.generateNamespaceAliases(unit))
      .getOrElse(Namespace.staticAliases)
  }

  /** Print ast to writer. */
  def renderToWriter[W: Output](writer: W)(implicit executor: ExecutionContext): Future[Unit] = Future(render(writer))

  /** Print ast to string. */
  def renderToString(implicit executor: ExecutionContext): Future[String] = Future(render())

  /** Print ast to file. */
  def renderToFile(remote: Platform, path: String)(implicit executor: ExecutionContext): Future[Unit] =
    renderToString.map(remote.write(path, _))

  private def render[W: Output](writer: W): Unit = {
    ExecutionLog.log(s"AMFSerializer#render: Rendering to $mediaType ($vendor file) ${unit.location()}")
    vendor match {
      case Vendor.AMF.name =>
        legacyOptions.toGraphSerialization match {
          case RdfSerialization()                => emitRdf(writer)
          case JsonLdSerialization(documentForm) => emitJsonLd(writer, documentForm)
        }
      case _ =>
        val ast = renderAsYDocument()
        AMFPluginsRegistry.syntaxPluginForMediaType(mediaType) match {
          case Some(syntaxPlugin) =>
            syntaxPlugin.unparse(mediaType, ast, writer)
            notifyEvent(FinishedRenderingSyntaxEvent(unit))
          case None if unit.isInstanceOf[ExternalFragment] =>
            writer.append(unit.asInstanceOf[ExternalFragment].encodes.raw.value())
          case _ => throw new Exception(s"Unsupported media type $mediaType and vendor $vendor")
        }
    }
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
        val d = RdfModelDocument(r.unitToRdfModel(unit, legacyOptions))
        RdfSyntaxPlugin.unparse(mediaType, d, writer)
      case _ => None
    }

  private def render(): String = {
    val w = new StringWriter
    render(w)
    w.toString
  }

  private def getRenderPlugin: AMFRenderPlugin = {
    val renderPlugin = config.renderPlugins.sorted.find(_.applies(RenderInfo(unit, vendor, mediaType)))
    renderPlugin.getOrElse {
      throw new Exception(
          s"Cannot serialize domain model '${unit.location()}' for detected media type $mediaType and vendor $vendor")
    }
  }
}

object AMFSerializer {

  private def generateRenderEnv(options: Option[RenderOptions],
                                shapeOptions: ShapeRenderOptions): RenderConfiguration = {
    val renderOptions    = options.getOrElse(RenderOptions())
    val immutableOptions = RenderOptions.toImmutable(renderOptions, ShapeRenderOptions.toImmutable(shapeOptions))
    val errorHandler     = options.map(_.errorHandler).getOrElse(shapeOptions.errorHandler)
    val env = AMFPluginsRegistry
      .obtainStaticConfig()
      .withRenderOptions(immutableOptions)
      .withErrorHandlerProvider(() => errorHandler)
    DefaultRenderConfiguration(env)
  }

  def init()(implicit executionContext: ExecutionContext): Unit = {
    RuntimeSerializer.register(
        new RuntimeSerializer {
          override def dump(unit: BaseUnit,
                            mediaType: String,
                            vendor: String,
                            options: RenderOptions,
                            shapeOptions: ShapeRenderOptions): String =
            new AMFSerializer(unit, mediaType, vendor, options, shapeOptions).render()

          override def dump(unit: BaseUnit,
                            mediaType: String,
                            vendor: String,
                            shapeOptions: ShapeRenderOptions): String = {
            new AMFSerializer(unit, mediaType, vendor, shapeOptions).render()
          }

          override def dumpToFile(platform: Platform,
                                  file: String,
                                  unit: BaseUnit,
                                  mediaType: String,
                                  vendor: String,
                                  options: RenderOptions,
                                  shapeOptions: ShapeRenderOptions): Future[Unit] = {
            new AMFSerializer(unit, mediaType, vendor, options, shapeOptions).renderToFile(platform, file)
          }
        })
  }
}
