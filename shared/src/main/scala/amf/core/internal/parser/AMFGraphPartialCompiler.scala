package amf.core.internal.parser

import amf.core.client.scala.exception.UnsupportedParsedDocumentException
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfObject, DomainElement}
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.common.{HighPriority, PluginPriority}
import amf.core.internal.metamodel.Obj
import amf.core.internal.metamodel.document.{BaseUnitModel, DocumentModel}
import amf.core.internal.metamodel.domain.DomainElementModel
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler, SyamlParsedDocument}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.internal.plugins.document.graph.parser.{
  FlattenedGraphParser,
  FlattenedUnitGraphParser,
  GraphDependenciesReferenceHandler,
  GraphParserContext
}
import amf.core.internal.remote.Spec.AMF
import amf.core.internal.remote.{Mimes, Spec}

class AMFGraphPartialCompiler(compilerContext: CompilerContext, startingPoint: String)
    extends AMFCompiler(compilerContext) {

  private case class PartialGraphParsePlugin() extends AMFParsePlugin {

    private case class EmptyDomainElement() extends DomainElement {
      override def meta: Obj = DomainElementModel

      /** Set of fields composing object. */
      override val fields: Fields = Fields()

      /** Value , path + field value that is used to compose the id when the object its adopted */
      private[amf] override def componentId: String = "error"

      /** Set of annotations for element. */
      override val annotations: Annotations = Annotations()
      withId("amf://error-domain-element")
    }

    override def parse(document: Root, ctx: ParserContext): BaseUnit = {
      document.parsed match {
        case s: SyamlParsedDocument =>
          val parsed = new FlattenedGraphParser(startingPoint)(new GraphParserContext(config = ctx.config))
            .parse(s.document) match {
            case Some(obj) => obj
            case _         => EmptyDomainElement()
          }

          AmfObjectUnitContainer(parsed)

        case _ => throw UnsupportedParsedDocumentException
      }
    }

    /**
      * media types which specifies vendors that are parsed by this plugin.
      */
    override def mediaTypes: Seq[String] = Seq(Mimes.`application/graph`, Mimes.`application/ld+json`)

    override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = GraphDependenciesReferenceHandler

    override def allowRecursiveReferences: Boolean = true

    override val id: String = "PartialGraphPlugin"

    override def applies(element: Root): Boolean = true

    override def priority: PluginPriority = HighPriority

    override def spec: Spec = AMF

    override def referencePlugins: Seq[AMFParsePlugin] = List(this)
  }
  private val parsePlugin = PartialGraphParsePlugin()

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
  private[amf] override def componentId: String = ""
}
