package amf.core

import java.io.StringWriter

import amf.client.plugins.AMFDocumentPlugin
import amf.core.benchmark.ExecutionLog
import amf.core.emitter.{RenderOptions, ShapeRenderOptions}
import amf.core.model.document.{BaseUnit, ExternalFragment}
import amf.core.parser.SyamlParsedDocument
import amf.core.rdf.RdfModelDocument
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote.{Platform, Vendor}
import amf.core.services.RuntimeSerializer
import amf.plugins.document.graph.AMFGraphPlugin.platform
import amf.plugins.document.graph.emitter.{FlattenedJsonLdEmitter, JsonLdEmitter}
import amf.plugins.document.graph.parser.{
  ExpandedForm,
  FlattenedForm,
  JsonLdDocumentForm,
  JsonLdSerialization,
  RdfSerialization
}
import amf.plugins.syntax.RdfSyntaxPlugin
import org.mulesoft.common.io.Output
import org.mulesoft.common.io.Output._
import org.yaml.builder.{DocBuilder, JsonOutputBuilder, YDocumentBuilder}
import org.yaml.model.YDocument

import scala.concurrent.{ExecutionContext, Future}

class AMFSerializer(unit: BaseUnit,
                    mediaType: String,
                    vendor: String,
                    options: RenderOptions,
                    shapeOptions: ShapeRenderOptions = ShapeRenderOptions()) {

  def renderAsYDocument(): SyamlParsedDocument = {
    val domainPlugin = getDomainPlugin
    val builder      = new YDocumentBuilder
    if (domainPlugin.emit(unit, builder, options, shapeOptions))
      SyamlParsedDocument(builder.result.asInstanceOf[YDocument])
    else throw new Exception(s"Error unparsing syntax $mediaType with domain plugin ${domainPlugin.ID}")
  }

  /** Render to doc builder. */
  def renderToBuilder[T](builder: DocBuilder[T])(implicit executor: ExecutionContext): Future[Unit] = Future {
    vendor match {
      case Vendor.AMF.name =>
        options.toGraphSerialization match {
          case JsonLdSerialization(FlattenedForm) => FlattenedJsonLdEmitter.emit(unit, builder, options)
          case JsonLdSerialization(ExpandedForm)  => JsonLdEmitter.emit(unit, builder, options)
        }
    }
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
        options.toGraphSerialization match {
          case RdfSerialization()                => emitRdf(writer)
          case JsonLdSerialization(documentForm) => emitJsonLd(writer, documentForm)
        }
      case _ =>
        val ast = renderAsYDocument()
        AMFPluginsRegistry.syntaxPluginForMediaType(mediaType) match {
          case Some(syntaxPlugin) => syntaxPlugin.unparse(mediaType, ast, writer)
          case None if unit.isInstanceOf[ExternalFragment] =>
            writer.append(unit.asInstanceOf[ExternalFragment].encodes.raw.value())
          case _ => throw new Exception(s"Unsupported media type $mediaType and vendor $vendor")
        }
    }
  }

  private def emitJsonLd[W: Output](writer: W, form: JsonLdDocumentForm): Unit = {
    val b = JsonOutputBuilder[W](writer, options.isPrettyPrint)
    form match {
      case FlattenedForm => FlattenedJsonLdEmitter.emit(unit, b, options)
      case ExpandedForm  => JsonLdEmitter.emit(unit, b, options)
      case _             => // Ignore
    }
  }

  private def emitRdf[W: Output](writer: W): Unit =
    platform.rdfFramework match {
      case Some(r) =>
        val d = RdfModelDocument(r.unitToRdfModel(unit, options))
        RdfSyntaxPlugin.unparse(mediaType, d, writer)
      case _ => None
    }

  private def render(): String = {
    val w = new StringWriter
    render(w)
    w.toString
  }

  protected def findDomainPlugin(): Option[AMFDocumentPlugin] =
    AMFPluginsRegistry.documentPluginForVendor(vendor).find { plugin =>
      plugin.documentSyntaxes.contains(mediaType) && plugin.canUnparse(unit)
    } match {
      case Some(domainPlugin) =>
        Some(domainPlugin)
      case None => AMFPluginsRegistry.documentPluginForMediaType(mediaType).find(_.canUnparse(unit))
    }

  private def getDomainPlugin: AMFDocumentPlugin =
    findDomainPlugin().getOrElse {
      throw new Exception(
        s"Cannot serialize domain model '${unit.location()}' for detected media type $mediaType and vendor $vendor")
    }
}

object AMFSerializer {
  def init()(implicit executionContext: ExecutionContext): Unit = {
    RuntimeSerializer.register(new RuntimeSerializer {
      override def dump(unit: BaseUnit,
                        mediaType: String,
                        vendor: String,
                        options: RenderOptions,
                        shapeOptions: ShapeRenderOptions): String =
        new AMFSerializer(unit, mediaType, vendor, options, shapeOptions).render()

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
