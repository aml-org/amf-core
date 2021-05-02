package amf.plugins.document.graph

import amf.client.plugins.{AMFDocumentPlugin, AMFPlugin}
import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.parse.AMFParsePluginAdapter
import amf.client.remod.amfcore.plugins.render.AMFRenderPluginAdapter
import amf.core.Root
import amf.core.client.ParsingOptions
import amf.core.errorhandling.ErrorHandler
import amf.core.exception.UnsupportedParsedDocumentException
import amf.core.metamodel.Obj
import amf.core.metamodel.domain._
import amf.core.model.document.BaseUnit
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.parser._
import amf.core.rdf.{RdfModelDocument, RdfModelParser}
import amf.core.remote.Amf
import amf.core.resolution.pipelines.{BasicEditingResolutionPipeline, BasicResolutionPipeline, ResolutionPipeline}
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.graph.emitter.EmbeddedJsonLdEmitter
import amf.plugins.document.graph.parser.{EmbeddedGraphParser, FlattenedGraphParser, GraphDependenciesReferenceHandler}
import org.yaml.builder.DocBuilder
import org.yaml.model.YDocument

import scala.concurrent.{ExecutionContext, Future}

object AMFGraphParsePlugin  extends AMFParsePluginAdapter(AMFGraphPlugin)
object AMFGraphRenderPlugin extends AMFRenderPluginAdapter(AMFGraphPlugin)

object AMFGraphPlugin extends AMFDocumentPlugin with PlatformSecrets {

  override def init()(implicit executionContext: ExecutionContext): Future[AMFPlugin] = Future { this }

  override val ID: String                   = Amf.name
  override def dependencies(): Seq[Nothing] = Seq()

  override val validVendorsToReference: Seq[String] = Nil

  val vendors: Seq[String] = Seq(Amf.name)

  override def modelEntities: Seq[Obj] = Seq(
      ObjectNodeModel,
      ScalarNodeModel,
      ArrayNodeModel,
      LinkNodeModel,
      RecursiveShapeModel
  )

  override def serializableAnnotations(): Map[String, AnnotationGraphLoader] = Map.empty

  override def documentSyntaxes: Seq[String] = Seq(
      "application/ld+json",
      "application/json",
      "application/amf+json"
  )

  override def canParse(root: Root): Boolean = {
    root.parsed match {
      case parsed: SyamlParsedDocument =>
        FlattenedGraphParser.canParse(parsed) || EmbeddedGraphParser.canParse(parsed)
      case _: RdfModelDocument => true

      case _ => false
    }
  }

  override def parse(root: Root, ctx: ParserContext, options: ParsingOptions): BaseUnit =
    root.parsed match {
      case parsed: SyamlParsedDocument if FlattenedGraphParser.canParse(parsed) =>
        FlattenedGraphParser(ctx.eh).parse(parsed.document, effectiveUnitUrl(root.location, options))
      case parsed: SyamlParsedDocument if EmbeddedGraphParser.canParse(parsed) =>
        EmbeddedGraphParser(ctx.eh).parse(parsed.document, effectiveUnitUrl(root.location, options))
      case parsed: RdfModelDocument =>
        RdfModelParser(ctx.eh).parse(parsed.model, effectiveUnitUrl(root.location, options))
      case _ =>
        throw UnsupportedParsedDocumentException
    }

  override def canUnparse(unit: BaseUnit) = true

  override def emit[T](unit: BaseUnit,
                       builder: DocBuilder[T],
                       renderOptions: RenderOptions,
                       errorHandler: ErrorHandler): Boolean =
    EmbeddedJsonLdEmitter.emit(unit, builder, renderOptions)

  override protected def unparseAsYDocument(unit: BaseUnit,
                                            renderOptions: RenderOptions,
                                            errorHandler: ErrorHandler): Option[YDocument] =
    throw new IllegalStateException("Unreachable")

  override def referenceHandler(eh: ErrorHandler): ReferenceHandler = GraphDependenciesReferenceHandler

  override val pipelines: Map[String, ResolutionPipeline] = Map(
      BasicResolutionPipeline.name        -> BasicResolutionPipeline(),
      BasicEditingResolutionPipeline.name -> BasicEditingResolutionPipeline() // hack to maintain compatibility with legacy behaviour
  )

  /**
    * Does references in this type of documents be recursive?
    */
  override val allowRecursiveReferences: Boolean = true

  protected def effectiveUnitUrl(location: String, options: ParsingOptions): String = {
    options.definedBaseUrl match {
      case Some(url) => url
      case None      => location
    }
  }

}
