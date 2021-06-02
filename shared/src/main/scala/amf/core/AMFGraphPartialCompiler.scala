package amf.core

import amf.client.remod.amfcore.plugins.parse.AMFParsePlugin
import amf.client.remod.amfcore.plugins.{HighPriority, PluginPriority}
import amf.core.errorhandling.AMFErrorHandler
import amf.core.exception.UnsupportedParsedDocumentException
import amf.core.metamodel.Obj
import amf.core.metamodel.Type.ObjType
import amf.core.metamodel.document.{BaseUnitModel, DocumentModel}
import amf.core.metamodel.domain.DomainElementModel
import amf.core.model.document.{BaseUnit, Document}
import amf.core.model.domain.{AmfObject, DomainElement}
import amf.core.parser.{Annotations, Fields, ParserContext, ReferenceHandler, SyamlParsedDocument}
import amf.plugins.document.graph.parser.{
  FlattenedGraphParser,
  FlattenedUnitGraphParser,
  GraphDependenciesReferenceHandler,
  GraphParserContext
}

class AMFGraphPartialCompiler(compilerContext: CompilerContext, startingPoint: String)
    extends AMFCompiler(compilerContext, Some("application/graph+json")) {

  implicit val executionContext = compilerContext.parserContext.config.executionContext

  private val parsePlugin = new AMFParsePlugin {
    override def parse(document: Root, ctx: ParserContext): BaseUnit = {
      document.parsed match {
        case s: SyamlParsedDocument =>
          val maybeObject =
            new FlattenedGraphParser(ctx.config, startingPoint)(new GraphParserContext(eh = ctx.config.eh))
              .parse(s.document)
          val obj = maybeObject match {
            case Some(obj) => obj
            case _ =>
              new DomainElement {
                override def meta: Obj = DomainElementModel

                /** Set of fields composing object. */
                override val fields: Fields = Fields()

                /** Value , path + field value that is used to compose the id when the object its adopted */
                override def componentId: String = "error"

                /** Set of annotations for element. */
                override val annotations: Annotations = Annotations()
                withId("amf://error-domain-element")
              }
          }
          AmfObjectUnitContainer(obj)

        case _ => throw UnsupportedParsedDocumentException
      }
    }

    /**
      * media types which specifies vendors that are parsed by this plugin.
      */
    override def mediaTypes: Seq[String] = Seq("application/graph+json")

    /**
      * media types which specifies vendors that may be referenced.
      */
    override def validMediaTypesToReference: Seq[String] = Nil

    override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = GraphDependenciesReferenceHandler

    override def allowRecursiveReferences: Boolean = true

    override val id: String = "PartialGraphPlugin"

    override def applies(element: Root): Boolean = true

    override def priority: PluginPriority = HighPriority
  }

  override def getDomainPluginFor(document: Root): Option[AMFParsePlugin] = {
    document.parsed match {
      case s: SyamlParsedDocument if FlattenedUnitGraphParser.canParse(s) => Some(parsePlugin)
      case _                                                              => None
    }
  }
}

private[amf] case class AmfObjectUnitContainer(result: AmfObject,
                                               override val fields: Fields = Fields(),
                                               override val annotations: Annotations = Annotations())
    extends BaseUnit {

  /** Meta data for the document */
  override def meta: BaseUnitModel = DocumentModel

  /** Returns the list document URIs referenced from the document that has been parsed to generate this model */
  override def references: Seq[BaseUnit] = Nil

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = ""
}
