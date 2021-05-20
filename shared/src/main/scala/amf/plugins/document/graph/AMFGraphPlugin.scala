package amf.plugins.document.graph

import amf.client.plugins.{AMFDocumentPlugin, AMFPlugin}
import amf.client.remod.amfcore.config.{ParsingOptions, RenderOptions}
import amf.client.remod.amfcore.plugins.parse.AMFParsePluginAdapter
import amf.client.remod.amfcore.plugins.render.AMFRenderPluginAdapter
import amf.core.Root
import amf.core.errorhandling.AMFErrorHandler
import amf.core.exception.UnsupportedParsedDocumentException
import amf.core.metamodel.Obj
import amf.core.model.document.BaseUnit
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.parser._
import amf.core.rdf.{RdfModelDocument, RdfModelParser}
import amf.core.remote.Amf
import amf.core.resolution.pipelines.{
  BasicEditingTransformationPipeline,
  BasicTransformationPipeline,
  TransformationPipeline
}
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.graph.emitter.EmbeddedJsonLdEmitter
import amf.plugins.document.graph.entities.AMFGraphEntities
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

  override def modelEntities: Seq[Obj] = AMFGraphEntities.entities.values.toSeq

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

  override def parse(root: Root, ctx: ParserContext): BaseUnit =
    root.parsed match {
      case parsed: SyamlParsedDocument if FlattenedGraphParser.canParse(parsed) =>
        FlattenedGraphParser(ctx.config).parse(parsed.document, effectiveUnitUrl(root.location, ctx.parsingOptions))
      case parsed: SyamlParsedDocument if EmbeddedGraphParser.canParse(parsed) =>
        EmbeddedGraphParser(ctx.config).parse(parsed.document, effectiveUnitUrl(root.location, ctx.parsingOptions))
      case parsed: RdfModelDocument =>
        RdfModelParser(ctx.config).parse(parsed.model, effectiveUnitUrl(root.location, ctx.parsingOptions))
      case _ => throw UnsupportedParsedDocumentException
    }

  override def canUnparse(unit: BaseUnit) = true

  override def emit[T](unit: BaseUnit,
                       builder: DocBuilder[T],
                       renderOptions: RenderOptions,
                       errorHandler: AMFErrorHandler): Boolean =
    EmbeddedJsonLdEmitter.emit(unit, builder, renderOptions)

  override protected def unparseAsYDocument(unit: BaseUnit,
                                            renderOptions: RenderOptions,
                                            errorHandler: AMFErrorHandler): Option[YDocument] =
    throw new IllegalStateException("Unreachable")

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = GraphDependenciesReferenceHandler

  override val pipelines: Map[String, TransformationPipeline] = Map(
      BasicTransformationPipeline.name        -> BasicTransformationPipeline(),
      BasicEditingTransformationPipeline.name -> BasicEditingTransformationPipeline() // hack to maintain compatibility with legacy behaviour
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
