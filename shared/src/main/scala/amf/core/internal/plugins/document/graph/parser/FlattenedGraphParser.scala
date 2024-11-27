package amf.core.internal.plugins.document.graph.parser

import amf.core.client.scala.errorhandling.IgnoringErrorHandler
import amf.core.client.scala.model.document._
import amf.core.client.scala.model.domain._
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.client.scala.vocabulary.Namespace.XsdTypes.xsdBoolean
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Type.{Array, Bool, Iri, LiteralUri, RegExp, SortedArray, Str}
import amf.core.internal.metamodel.document.BaseUnitModel
import amf.core.internal.metamodel.domain.{DomainElementModel, LinkableElementModel}
import amf.core.internal.metamodel.{Field, ModelDefaultBuilder, Obj, Type}
import amf.core.internal.parser._
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import amf.core.internal.plugins.document.graph.MetaModelHelper._
import amf.core.internal.plugins.document.graph.context.ExpandedTermDefinition
import amf.core.internal.validation.CoreValidations.UnableToParseDocument
import org.yaml.model._

import scala.collection.mutable
import scala.language.implicitConversions

class FlattenedUnitGraphParser(overrideAliases: Map[String, String] = Map.empty)(implicit val ctx: GraphParserContext)
    extends GraphParserHelpers {

  protected def parserProvider(
      rootId: String,
      overrideAliases: Map[String, String],
      ctx: GraphParserContext
  ): FlattenedGraphParser = new FlattenedGraphParser(rootId, overrideAliases)(ctx)

  def parse(document: YDocument, location: String): BaseUnit = {

    val rootNode: Option[YNode] = FlattenedUnitGraphParser.findRootNode(document)

    val unit = rootNode.flatMap(_.toOption[YMap]).flatMap(m => retrieveId(m, ctx)) match {
      case Some(rootId) =>
        parserProvider(rootId, overrideAliases, ctx).parse(document) match {
          case Some(b: BaseUnit) => b
          case Some(_) =>
            ctx.eh.violation(UnableToParseDocument, "", "Root node is not a Base Unit")
            Document()

          case _ =>
            ctx.eh.violation(UnableToParseDocument, "", "Unable to parse root node")
            Document()

        }
      case _ =>
        ctx.eh.violation(UnableToParseDocument, "", "Error parsing root JSON-LD node")
        Document()
    }
    unit.withLocation(location)
  }
}

class FlattenedGraphParser(startingPoint: String, overrideAliases: Map[String, String] = Map.empty)(implicit
    val ctx: GraphParserContext
) extends CommonGraphParser {

  private lazy val extensions = ctx.config.registryContext.getRegistry.getEntitiesRegistry.extensionTypes
  private lazy val extensionFields = extensions.map { case (iriDomain, extensions) =>
    iriDomain -> extensions.map { case (iri, fieldType) =>
      Field(fieldType, ValueType(iri))
    }
  }

  private val cache                                   = mutable.Map[String, AmfObject]()
  private val graphMap: mutable.HashMap[String, YMap] = mutable.HashMap.empty

  def parse(document: YDocument): Option[AmfObject] = {
    document.node.value match {
      case documentMap: YMap =>
        documentMap
          .key(JsonLdKeywords.Context)
          .foreach(e => JsonLdGraphContextParser(e.value, ctx).parse())
        stepOrAddAliasesFromOptions()
        documentMap.key(JsonLdKeywords.Graph).flatMap { e =>
          parseGraph(e.value)
        }
      case _ => None
    }
  }

  override protected def parse(map: YMap): Option[AmfObject] = {
    if (isReferenceNode(map)) {
      parseReferenceNode(map)
    } else {
      for {
        id           <- retrieveId(map, ctx)
        model        <- retrieveType(id, map)
        parsedObject <- parseNode(map, id, model)
      } yield {
        parsedObject
      }
    }
  }

  private def stepOrAddAliasesFromOptions(): Unit = {
    overrideAliases.foreach { case (alias, uri) =>
      ctx.graphContext.withTerm(alias, uri)
    }
  }

  private def isSelfEncoded(node: YNode) =
    nodeIsOfType(node, BaseUnitModel) && nodeIsOfType(node, DomainElementModel)

  private def parseGraph(graph: YNode): Option[AmfObject] = {
    populateGraphMap(graph.as[YSequence])
    graphMap.get(adaptUriToContext(startingPoint)) match {
      case Some(rootNode) if isSelfEncoded(rootNode) =>
        parseSelfEncodedBaseUnit(rootNode)
      case Some(rootNode) => parseRoot(rootNode, startingPoint)
      case None =>
        ctx.eh.violation(UnableToParseDocument, "", "Cannot find root node for flattened JSON-LD")
        None
    }
  }

  private def parseRootNodeWithModel(rootNode: YMap, model: ModelDefaultBuilder) = {
    for {
      id      <- retrieveId(rootNode, ctx)
      sources <- Option(retrieveSources(rootNode))
      finalId <- Option(transformIdFromContext(id))
      parsed <- {
        val instance = buildType(model, annotations(nodes, sources, finalId))
        parseNodeFields(rootNode, fieldsFrom(model), sources, finalId, instance)
      }
    } yield {
      cache(id) = parsed
      parsed
    }

  }

  private def parseSelfEncodedBaseUnit(rootNode: YMap): Option[BaseUnit] = {
    val parsed = for {
      id           <- retrieveId(rootNode, ctx)
      encodesModel <- retrieveTypeIgnoring(id, rootNode, documentIris)
      // we need to pass the doc namespaces so a change in the order of the declaration of a self-encoded domain-element
      // does not take precedence over the AML document models
      unitModel <- retrieveTypeFrom(id, rootNode, documentIris)
      _         <- parseRootNodeWithModel(rootNode, encodesModel)
      baseUnit  <- parseRootNodeWithModel(rootNode, unitModel)
    } yield {
      baseUnit
    }

    baseUnitOrError(parsed)
  }

  private def parseRoot(rootNode: YMap, id: String): Option[AmfObject] = {
    for {
      model  <- retrieveType(id, rootNode)
      parsed <- parseNode(rootNode, id, model)
    } yield {
      parsed
    }
  }

  private def populateGraphMap(graphNodes: YSequence): Unit = {
    def toMapEntry(node: YNode): Option[(String, YMap)] = {
      val map = node.as[YMap] // All nodes at root level should be objects
      retrieveId(map, ctx) match {
        case Some(id) => Some((id, map))
        case _        => None
      }
    }
    graphNodes.nodes.flatMap { toMapEntry }.foreach { case (id, map) =>
      graphMap(id) = map
    }
  }

  private def retrieveType(id: String, map: YMap): Option[ModelDefaultBuilder] = retrieveTypeIgnoring(id, map, Nil)

  private def retrieveTypeFrom(id: String, map: YMap, from: Seq[ValueType]): Option[ModelDefaultBuilder] = {
    val expectedIris = from.map(_.iri())
    this.retrieveTypeCondition(id, map, t => expectedIris.exists(iri => equal(t, iri)(ctx.graphContext)))
  }

  private def retrieveTypeIgnoring(id: String, map: YMap, ignored: Seq[ValueType]): Option[ModelDefaultBuilder] = {
    val ignoredIris = ignored.map(_.iri())
    this.retrieveTypeCondition(id, map, t => !ignoredIris.exists(iri => equal(t, iri)(ctx.graphContext)))
  }

  /** Returns the first type defined in the @type entry from a YMap that matches the given predicate
    * @param id
    *   id for error reporting
    * @param map
    *   input ymap
    * @param pred
    *   predicate
    * @return
    *   Option for the first matching type as an Obj
    */
  private def retrieveTypeCondition(id: String, map: YMap, pred: String => Boolean): Option[ModelDefaultBuilder] = {
    // this returns a certain order, we will return the first one that matches, but many could match
    // first non-documents (including AML documents, dialect instances, dialects, vocabs, etc) are returned
    // then the base document models are returned in sorted order: Document, Fragment, Module
    val typeIris = ts(map, id)
      .filter(pred)              // we are filtering the list with the provided condition
      .map(expandUriFromContext) // expand iris

    findType(typeIris, id, map)
  }

  private def parseSortedArray(listType: Type, rawNode: YMap): Seq[AmfElement] = {
    val members: List[YNode] = rawNode.key(JsonLdKeywords.List) match {
      case Some(node) =>
        // get members of the @list
        getListMembers(node)

      case None =>
        // get members of the sorted array implementation that uses a map with entries with _i for index
        contentOfNode(rawNode) match {
          case Some(node) => getArrayMapMembers(node)
          case None       => List.empty
        }
    }
    parseListMembers(members, listType)
  }

  private def parseNode(map: YMap, id: String, model: ModelDefaultBuilder): Option[AmfObject] = {
    val sources               = retrieveSources(map)
    val transformedId: String = transformIdFromContext(id)

    val builder = buildType(model, annotations(nodes, sources, transformedId))
    cache(id) = builder
    val fields = getMetaModelFields(model)

    parseNodeFields(map, fields, sources, transformedId, builder)
  }

  private def getMetaModelFields(model: ModelDefaultBuilder): Seq[Field] = {
    fieldsFrom(model) ++ extensionsFor(model)
  }

  private def extensionsFor(model: ModelDefaultBuilder): Seq[Field] = {
    model.`type`.flatMap(valueType => extensionFields.get(valueType.iri())).flatten
  }

  private def parseReferenceNode(node: YMap): Option[AmfObject] = {
    retrieveId(node, ctx).flatMap { id =>
      cache.get(id) match {
        case Some(parsed) => Some(parsed)
        case None         =>
          // Cache miss
          nodeFromId(id).flatMap(parse)
      }
    }
  }

  override protected def contentOfNode(n: YNode): Option[YMap] = {
    val id: Option[String] = n.asOption[YMap].flatMap(retrieveId(_, ctx))
    id.flatMap(nodeFromId)
  }

  private def nodeFromId(id: String): Option[YMap] = {
    graphMap.get(id) match {
      case Some(raw) => Some(raw)
      case None =>
        ctx.eh.violation(UnableToParseDocument, "", s"Cannot find node with $id")
        None
    }
  }

  override protected def parseLinkableProperties(map: YMap, instance: DomainElement with Linkable): Unit = {
    val targetIdFieldIri = LinkableElementModel.TargetId.value.iri()
    map
      .key(compactUriFromContext(targetIdFieldIri))
      .flatMap(entry => {
        entry.value.tagType match {
          case YType.Map => retrieveId(entry.value.as[YMap], ctx)
          case YType.Seq => retrieveId(entry.value.as[Seq[YMap]].head, ctx)
          case _ =>
            ctx.eh.violation(
              UnableToParseDocument,
              entry.value,
              s"$targetIdFieldIri field must have a map or array value"
            )
            None
        }
      })
      .foreach { targetId =>
        val transformedId = transformIdFromContext(targetId)
        setLinkTarget(instance, transformedId)
      }

    mapLinkableProperties(map, instance)
  }

  override protected def parseObjectNodeProperties(obj: ObjectNode, map: YMap, fields: Seq[Field]): Unit = {
    val ignoredFields: Seq[String] =
      Seq(
        JsonLdKeywords.Id,
        JsonLdKeywords.Type,
        "smaps",
        DomainElementModel.Sources.value.iri(),
        (Namespace.Document + "name").iri(),
        (Namespace.Core + "extensionName").iri()
      )

    map.entries.foreach { entry =>
      val fieldUri        = expandUriFromContext(entry.key.as[String])
      val fieldValue      = entry.value
      val isAlreadyParsed = fields.exists(_.value.iri() == fieldUri)
      if (
        !ignoredFields
          .contains(fieldUri) && !isAlreadyParsed
      ) { // we do this to prevent parsing name of annotations
        parse(fieldValue.as[YMap]).collect { case d: amf.core.client.scala.model.domain.DataNode =>
          obj.addProperty(fieldUri, d)
        }
      }
    }
  }

  private def isReferenceNode(m: YMap): Boolean = m.entries.size == 1 && m.key(JsonLdKeywords.Id).isDefined

  override protected def parseAtTraversion(node: YNode, `type`: Type): Option[AmfElement] = {
    `type` match {
      case _: Obj                    => parse(node.as[YMap])
      case Iri                       => Some(iri(node))
      case Str | RegExp | LiteralUri => Some(str(node))
      case Bool                      => Some(bool(node))
      case Type.Int                  => Some(int(node))
      case Type.Float                => Some(double(node))
      case Type.Double               => Some(double(node))
      case Type.Long                 => Some(long(node))
      case Type.DateTime             => Some(date(node))
      case Type.Date                 => Some(date(node))
      case Type.Any                  => Some(any(node))
      case l: SortedArray            => Some(AmfArray(parseSortedArray(l.element, node.as[YMap])))
      case a: Array =>
        (node.tagType, a.element) match {
          case (YType.Seq, _) => yNodeSeq(node, a)
          case (YType.Map, _: Obj) =>
            val rawHead = node.as[YMap]
            parse(rawHead).map(o => AmfArray(Seq(o)))
          case (YType.Str, Str | Iri) =>
            Some(AmfArray(Seq(str(node))))
          case _ => None
        }
    }
  }

  private def baseUnitOrError(parsed: Option[AmfObject]) = {
    parsed match {
      case Some(b: BaseUnit) =>
        Some(b)
      case Some(_) =>
        ctx.eh.violation(UnableToParseDocument, "", "Root node is not a Base Unit")
        None
      case _ =>
        ctx.eh.violation(UnableToParseDocument, "", "Unable to parse root node")
        None
    }
  }

  private def buildType(modelType: ModelDefaultBuilder, ann: Annotations): AmfObject = {
    val instance = modelType.modelInstance
    instance.annotations ++= ann
    instance
  }
}

object FlattenedUnitGraphParser extends GraphContextHelper with GraphParserHelpers {

  def apply(config: ParseConfiguration, aliases: Map[String, String] = Map.empty): FlattenedUnitGraphParser = {
    new FlattenedUnitGraphParser(aliases)(new GraphParserContext(config = config))
  }
  implicit val ctx: GraphParserContext = new GraphParserContext(
    config = LimitedParseConfig(IgnoringErrorHandler)
  )

  /** Returns true if `document` contains a @graph node which in turn must contain a Root node. Root nodes are nodes
    * with @type http://a.ml/vocabularies/document#Unit (BaseUnits) with the property
    * http://a.ml/vocabularies/document#root set to true.
    * @param document
    *   document to perform the canParse test on
    * @return
    */
  def canParse(document: SyamlParsedDocument, aliases: Map[String, String] = Map.empty): Boolean =
    findRootNode(document.document, aliases).isDefined

  private def isRootNode(node: YNode)(implicit ctx: GraphParserContext): Boolean = {
    node.value match {
      case map: YMap =>
        val isBaseUnit = nodeIsOfType(node, BaseUnitModel)
        lazy val isRoot = map.entries.exists(entry => {
          val key       = entry.key.as[String]
          val isRootIri = equal(BaseUnitModel.Root.value.iri(), key)(ctx.graphContext)
          val isRootValue = entry.value.tagType match {
            case YType.Str =>
              ctx.graphContext.define(key) match {
                case Some(expandedTermDefinition: ExpandedTermDefinition) =>
                  expandedTermDefinition.`type`.exists { t =>
                    equal(t, xsdBoolean.iri())(ctx.graphContext) && entry.value.as[String] == "true"
                  }
                case _ => false
              }
            case YType.Bool =>
              entry.value.as[Boolean]
            case _ => false
          }
          isRootIri && isRootValue
        })
        isBaseUnit && isRoot
      case _ => false
    }
  }

  private[amf] def findRootNode(document: YDocument, aliases: Map[String, String] = Map.empty): Option[YNode] = {
    document.node.value match {
      case m: YMap =>
        processGraph(m, ctx, aliases)
      case _ => None
    }
  }

  private[amf] def processGraph(m: YMap, ctx: GraphParserContext, aliases: Map[String, String]): Option[YNode] = {
    m.key(JsonLdKeywords.Context).foreach { entry =>
      try {
        JsonLdGraphContextParser(entry.value, ctx).parse()
      } catch {
        case _: Error => None
      }
    }
    ctx.addTerms(aliases)
    m.key(JsonLdKeywords.Graph).flatMap { graphEntry =>
      val graphYSeq = graphEntry.value.as[YSequence]
      graphYSeq.nodes.find { node =>
        isRootNode(node)(ctx)
      }
    }
  }
}
