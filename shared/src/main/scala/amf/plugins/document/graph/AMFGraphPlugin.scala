package amf.plugins.document.graph

import amf.client.plugins.{AMFDocumentPlugin, AMFPlugin}
import amf.client.remod.amfcore.config.RenderOptions
import amf.core.Root
import amf.core.errorhandling.AMFErrorHandler
import amf.core.metamodel.Obj
import amf.core.model.document.BaseUnit
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.parser._
import amf.core.remote.Amf
import amf.core.resolution.pipelines.{
  BasicEditingTransformationPipeline,
  BasicTransformationPipeline,
  TransformationPipeline
}
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.graph.emitter.EmbeddedJsonLdEmitter
import amf.plugins.document.graph.entities.AMFGraphEntities
import amf.plugins.parse.AMFGraphParsePlugin
import org.yaml.builder.DocBuilder
import org.yaml.model.YDocument

import scala.concurrent.{ExecutionContext, Future}

object AMFGraphPlugin extends AMFDocumentPlugin with PlatformSecrets {

  override def init()(implicit executionContext: ExecutionContext): Future[AMFPlugin] = Future { this }

  override val ID: String                   = Amf.name
  override def dependencies(): Seq[Nothing] = Seq()

  override val validVendorsToReference: Seq[String] = Nil

  val vendors: Seq[String] = AMFGraphParsePlugin.mediaTypes

  override def modelEntities: Seq[Obj] = AMFGraphEntities.entities.values.toSeq

  override def serializableAnnotations(): Map[String, AnnotationGraphLoader] = Map.empty

  override def documentSyntaxes: Seq[String] = Seq(
      Amf.mediaType,
      "application/ld+json",
      "application/json",
      "application/amf+json",
      "application/amf+json",
      "application/graph"
  )

  override def canParse(root: Root): Boolean = AMFGraphParsePlugin.applies(root)

  override def parse(root: Root, ctx: ParserContext): BaseUnit = AMFGraphParsePlugin.parse(root, ctx)

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

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = AMFGraphParsePlugin.referenceHandler(eh)

  override val pipelines: Map[String, TransformationPipeline] = Map(
      BasicTransformationPipeline.name        -> BasicTransformationPipeline(),
      BasicEditingTransformationPipeline.name -> BasicEditingTransformationPipeline() // hack to maintain compatibility with legacy behaviour
  )

  /**
    * Does references in this type of documents be recursive?
    */
  override val allowRecursiveReferences: Boolean = AMFGraphParsePlugin.allowRecursiveReferences
}
