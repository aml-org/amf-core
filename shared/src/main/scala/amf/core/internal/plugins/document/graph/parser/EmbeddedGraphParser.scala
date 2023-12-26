package amf.core.internal.plugins.document.graph.parser

import amf.core.client.scala.model.document._
import amf.core.client.scala.model.domain._
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.metamodel.Type.{Array, Bool, Iri, LiteralUri, RegExp, SortedArray, Str}
import amf.core.internal.metamodel._
import amf.core.internal.metamodel.document.BaseUnitModel.Location
import amf.core.internal.metamodel.domain._
import amf.core.internal.parser._
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import amf.core.internal.validation.CoreValidations.UnableToParseDocument
import org.yaml.convert.YRead.SeqNodeYRead
import org.yaml.model._

import scala.collection.mutable.ListBuffer

/** AMF Graph parser
  */
class EmbeddedGraphParser(private val aliases: Map[String, String])(implicit val ctx: GraphParserContext)
    extends CommonGraphParser {

  def canParse(document: SyamlParsedDocument): Boolean = EmbeddedGraphParser.canParse(document)

  def parse(document: YDocument, location: String): BaseUnit = {
    val parsedOption = for {
      seq  <- document.node.toOption[Seq[YMap]]
      head <- seq.headOption
      parsed <- {
        head.key(JsonLdKeywords.Context, e => JsonLdGraphContextParser(e.value, ctx).parse())
        aliases.foreach { case (term, iri) =>
          ctx.graphContext.withTerm(term, iri)
        }
        parse(head)
      }
    } yield {
      parsed
    }

    parsedOption match {
      case Some(unit: BaseUnit) => unit.set(Location, location)
      case _ =>
        ctx.eh.violation(UnableToParseDocument, location, s"Unable to parse $document", document.location)
        Document()
    }
  }

  override protected def parse(map: YMap): Option[AmfObject] = {
    retrieveId(map, ctx)
      .flatMap(value => retrieveType(value, map).map(value2 => (value, value2)))
      .flatMap {
        case (id, model) =>
          val sources               = retrieveSources(map)
          val transformedId: String = transformIdFromContext(id)

          val instance: AmfObject = buildType(model, annotations(nodes, sources, transformedId))

          // workaround for lazy values in shape
          val modelFields = model match {
            case shapeModel: ShapeModel =>
              shapeModel.fields ++ Seq(
                ShapeModel.CustomShapePropertyDefinitions,
                ShapeModel.CustomShapeProperties
              )
            case _ => model.fields
          }

          parseNodeFields(map, modelFields, sources, transformedId, instance)

        case _ => None
      }
  }

  private def retrieveType(id: String, map: YMap): Option[ModelDefaultBuilder] = {
    val stringTypes = ts(map, id)
    findType(stringTypes, id, map)
  }

  private def parseSortedArray(listType: Type, rawNode: YMap): Seq[AmfElement] = {
    val members: List[YNode] = rawNode.key(JsonLdKeywords.List) match {
      case Some(node) =>
        // get members of the @list
        getListMembers(node)
      case None =>
        // get members of the sorted array implementation that uses a map with entries with _i for index
        getArrayMapMembers(rawNode)
    }
    parseListMembers(members, listType)
  }

  override protected def parseLinkableProperties(map: YMap, instance: DomainElement with Linkable): Unit = {
    map
      .key(compactUriFromContext(LinkableElementModel.TargetId.value.iri()))
      .flatMap(entry => {
        retrieveId(entry.value.as[Seq[YMap]].head, ctx)
      })
      .foreach { targetId =>
        val transformedId = transformIdFromContext(targetId)
        setLinkTarget(instance, transformedId)
      }

    mapLinkableProperties(map, instance)
  }

  override protected def parseObjectNodeProperties(obj: ObjectNode, map: YMap, fields: Seq[Field]): Unit = {
    map.entries.foreach { entry =>
      val uri = expandUriFromContext(entry.key.as[String])
      val v   = entry.value
      if (
        uri != JsonLdKeywords.Type && uri != JsonLdKeywords.Id && uri != DomainElementModel.Sources.value
          .iri() && uri != "smaps" &&
        uri != (Namespace.Core + "extensionName").iri() && !fields
          .exists(_.value.iri() == uri)
      ) { // we do this to prevent parsing name of annotations
        v.as[Seq[YMap]]
          .headOption
          .flatMap(parse)
          .collect({ case d: amf.core.client.scala.model.domain.DataNode => obj.addProperty(uri, d) })
      }
    }
  }

  override protected def parseAtTraversion(node: YNode, `type`: Type): Option[AmfElement] = {
    `type` match {
      case _: Obj                    => parse(node.as[YMap])
      case Iri                       => Some(iri(node))
      case Str | RegExp | LiteralUri => Some(str(node))
      case Bool                      => Some(bool(node))
      case Type.Int                  => Some(int(node))
      case Type.Long                 => Some(long(node))
      case Type.Float                => Some(double(node))
      case Type.Double               => Some(double(node))
      case Type.DateTime             => Some(date(node))
      case Type.Date                 => Some(date(node))
      case Type.Any                  => Some(any(node))
      case l: SortedArray            => Some(AmfArray(parseSortedArray(l.element, node.as[YMap])))
      case a: Array                  => yNodeSeq(node, a)
    }
  }

  private def buildType(modelType: ModelDefaultBuilder, ann: Annotations): AmfObject = {
    val instance = modelType.modelInstance
    instance.annotations ++= ann
    instance
  }

}

object EmbeddedGraphParser {

  def apply(config: ParseConfiguration, aliases: Map[String, String]): EmbeddedGraphParser =
    new EmbeddedGraphParser(aliases)(new GraphParserContext(config = config))

  def canParse(document: SyamlParsedDocument): Boolean = {
    val maybeMaps = document.document.node.toOption[Seq[YMap]]
    val maybeMap  = maybeMaps.flatMap(s => s.headOption)
    maybeMap match {
      case Some(m: YMap) =>
        val toDocumentNamespace: String => String = a => (Namespace.Document + a).iri()
        val keys                                  = Seq("encodes", "declares", "references").map(toDocumentNamespace)
        val types = Seq("Document", "Fragment", "Module", "Unit").map(toDocumentNamespace)

        val acceptedKeys  = keys ++ keys.map(Namespace.defaultAliases.compact)
        val acceptedTypes = types ++ types.map(Namespace.defaultAliases.compact)
        acceptedKeys.exists(m.key(_).isDefined) ||
        m.key(JsonLdKeywords.Type).exists { typesEntry =>
          val retrievedTypes = typesEntry.value.asOption[YSequence].map(stringNodesFrom)
          retrievedTypes.exists(acceptedTypes.intersect(_).nonEmpty)
        }
      case _ => false
    }
  }

  private def stringNodesFrom(seq: YSequence): IndexedSeq[Any] =
    seq.nodes.flatMap(node => node.asOption[YScalar]).map(_.value)
}
