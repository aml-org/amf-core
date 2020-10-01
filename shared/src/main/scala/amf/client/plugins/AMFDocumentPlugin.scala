package amf.client.plugins

import amf.core.{CompilerContext, Root}
import amf.core.client.ParsingOptions
import amf.core.emitter.{RenderOptions, ShapeRenderOptions}
import amf.core.errorhandling.ErrorHandler
import amf.core.metamodel.Obj
import amf.core.model.document.BaseUnit
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.parser.{ParserContext, RefContainer, ReferenceHandler, ReferenceKind}
import amf.core.registries.AMFDomainEntityResolver
import amf.core.remote.{Platform, Vendor}
import amf.core.resolution.pipelines.ResolutionPipeline
import org.yaml.builder.{DocBuilder, YDocumentBuilder}
import org.yaml.model.{YDocument, YNode}

object AMFDocumentPluginSettings {
  object PluginPriorities {
    val high    = 0
    val default = 5
    val low     = 10
  }
}

abstract class AMFDocumentPlugin extends AMFPlugin {

  /**
    * Does references in this type of documents be recursive?
    */
  val allowRecursiveReferences: Boolean

  // Parameter modifying which plugins are tried first
  val priority: Int = AMFDocumentPluginSettings.PluginPriorities.default

  val vendors: Seq[String]

  val validVendorsToReference: Seq[Vendor] = Nil

  def modelEntities: Seq[Obj]

  def modelEntitiesResolver: Option[AMFDomainEntityResolver] = None

  def serializableAnnotations(): Map[String, AnnotationGraphLoader]

  def verifyValidFragment(refVendor: Option[Vendor], refs: Seq[RefContainer], ctx: CompilerContext): Unit = Unit

  def verifyReferenceKind(unit: BaseUnit,
                          definedKind: ReferenceKind,
                          allKinds: Seq[ReferenceKind],
                          nodes: Seq[YNode],
                          ctx: ParserContext): Unit = Unit

  /**
    * Resolves the provided base unit model, according to the semantics of the domain of the document
    */
  final def resolveWithHandle(unit: BaseUnit, pipelineId: String = ResolutionPipeline.DEFAULT_PIPELINE): BaseUnit =
    resolve(unit, unit.errorHandler(), pipelineId)

  /**
    * Resolves the provided base unit model, according to the semantics of the domain of the document
    */
  def resolve(unit: BaseUnit,
              errorHandler: ErrorHandler,
              pipelineId: String = ResolutionPipeline.DEFAULT_PIPELINE): BaseUnit

  /**
    * List of media types used to encode serialisations of
    * this domain
    */
  def documentSyntaxes: Seq[String]

  /**
    * Parses an accepted document returning an optional BaseUnit
    */
  def parse(document: Root, ctx: ParserContext, platform: Platform, options: ParsingOptions): Option[BaseUnit]


  /**
    * Emit an Output for a given base unit
    * The type of Output is Managed by the DocBuilder
    * Returns false if the document cannot be built
    */
  def emit[T](unit: BaseUnit,
              builder: DocBuilder[T],
              renderOptions: RenderOptions = RenderOptions(),
              shapeRenderOptions: ShapeRenderOptions = ShapeRenderOptions()): Boolean =
    builder match {
      case sb: YDocumentBuilder =>
        unparseAsYDocument(unit, renderOptions, shapeRenderOptions) exists { doc =>
          sb.document = doc
          true
        }
      case _ => false
    }

  protected def unparseAsYDocument(unit: BaseUnit,
                                   renderOptions: RenderOptions,
                                   shapeRenderOptions: ShapeRenderOptions): Option[YDocument]

  /**
    * Decides if this plugin can parse the provided document instance.
    * this can be used by multiple plugins supporting the same media-type
    * to decide which one will parse the document base on information from
    * the document structure
    */
  def canParse(document: Root): Boolean

  /**
    * Decides if this plugin can unparse the provided model document instance.
    * this can be used by multiple plugins supporting the same media-type
    * to decide which one will unparse the document base on information from
    * the instance type and properties
    */
  def canUnparse(unit: BaseUnit): Boolean

  def referenceHandler(eh: ErrorHandler): ReferenceHandler
}
