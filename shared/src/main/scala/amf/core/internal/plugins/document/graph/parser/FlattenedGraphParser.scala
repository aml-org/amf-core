package amf.core.internal.plugins.document.graph.parser
import amf.core.client.scala.config.ParsingOptions
import amf.core.client.scala.errorhandling.IgnoringErrorHandler
import amf.core.client.scala.model.document._
import amf.core.client.scala.model.domain._
import amf.core.client.scala.model.domain.extensions.{CustomDomainProperty, DomainExtension}
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.client.scala.vocabulary.Namespace.XsdTypes.xsdBoolean
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.annotations.DomainExtensionAnnotation
import amf.core.internal.metamodel.Type.{Array, Bool, Iri, LiteralUri, RegExp, SortedArray, Str}
import amf.core.internal.metamodel.document.BaseUnitModel
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel
import amf.core.internal.metamodel.domain.{DomainElementModel, ExternalSourceElementModel, LinkableElementModel}
import amf.core.internal.metamodel.{Field, ModelDefaultBuilder, Obj, Type}
import amf.core.internal.parser._
import amf.core.internal.parser.domain.{Annotations, FieldEntry}
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import amf.core.internal.plugins.document.graph.MetaModelHelper._
import amf.core.internal.plugins.document.graph.context.ExpandedTermDefinition
import amf.core.internal.validation.CoreValidations.{NotLinkable, UnableToParseDocument, UnableToParseNode}
import org.yaml.model._

import scala.collection.mutable
import scala.language.implicitConversions

class FlattenedUnitGraphParser(overrideAliases: Map[String, String] = Map.empty)(implicit val ctx: GraphParserContext)
    extends GraphParserHelpers {

  def parse(document: YDocument, location: String): BaseUnit = {

    val rootNode: Option[YNode] = FlattenedUnitGraphParser.findRootNode(document)

    val unit = rootNode.flatMap(_.toOption[YMap]).flatMap(m => retrieveId(m, ctx)) match {
      case Some(rootId) =>
        new FlattenedGraphParser(rootId, overrideAliases)(ctx).parse(document) match {
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

class FlattenedGraphParser(startingPoint: String, overrideAliases: Map[String, String] = Map.empty)(
    implicit val ctx: GraphParserContext)
    extends GraphParserHelpers {

  private lazy val extensions = ctx.config.registryContext.getRegistry.getEntitiesRegistry.extensionTypes
  private lazy val extensionFields = extensions.map {
    case (iriDomain, extensions) =>
      iriDomain -> extensions.map {
        case (iri, fieldType) => Field(fieldType, ValueType(iri))
      }
  }

  def parse(document: YDocument): Option[AmfObject] = {
    val parser = Parser(Map())
    parser.parse(document)
  }

  def annotations(nodes: Map[String, AmfElement], sources: SourceMap, key: String): Annotations =
    ctx.config.serializableAnnotationsFacade.retrieveAnnotation(nodes, sources, key)

  case class Parser(var nodes: Map[String, AmfElement]) extends GraphParserHelpers {
    private val unresolvedReferences = mutable.Map[String, Seq[DomainElement]]()
    private val unresolvedExtReferencesMap =
      mutable.Map[String, ExternalSourceElement]()

    private val referencesMap                           = mutable.Map[String, DomainElement]()
    private val cache                                   = mutable.Map[String, AmfObject]()
    private val graphMap: mutable.HashMap[String, YMap] = mutable.HashMap.empty

    def getRawNode(id: String): Option[YMap] = {
      graphMap.get(id) match {
        case None =>
          ctx.eh.violation(UnableToParseDocument, "", s"Cannot find node with $id")
          None
        case node => node
      }
    }

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

    private def stepOrAddAliasesFromOptions(): Unit = {
      overrideAliases.foreach {
        case (alias, uri) => ctx.graphContext.withTerm(alias, uri)
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
      graphNodes.nodes.flatMap { toMapEntry }.foreach {
        case (id, map) => graphMap(id) = map
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

    /**
      * Returns the first type defined in the @type entry from a YMap that matches the given predicate
      * @param id id for error reporting
      * @param map input ymap
      * @param pred predicate
      * @return Option for the first matching type as an Obj
      */
    private def retrieveTypeCondition(id: String, map: YMap, pred: String => Boolean): Option[ModelDefaultBuilder] = {
      // this returns a certain order, we will return the first one that matches, but many could match
      // first non-documents (including AML documents, dialect instances, dialects, vocabs, etc) are returned
      // then the base document models are returned in sorted order: Document, Fragment, Module
      val typeIris = ts(map, id)
        .filter(pred) // we are filtering the list with the provided condition
        .map(expandUriFromContext) // expand iris

      typeIris.find(findType(_).isDefined) match {
        case Some(t) => findType(t)
        case None =>
          ctx.eh
            .violation(UnableToParseNode, id, s"Error parsing JSON-LD node, unknown @types $typeIris", map.location)
          None
      }
    }

    private def parseSortedArray(listElement: Type, rawNode: YMap): Seq[AmfElement] = {
      def key(entry: YMapEntry): String = entry.key.as[String]
      contentOfNode(rawNode) match {
        case Some(node) =>
          // Sorted array members
          val members = node.entries.filter { entry =>
            val property           = key(entry)
            val sortedMemberPrefix = (Namespace.Rdfs + "_").iri()
            property.startsWith(compactUriFromContext(sortedMemberPrefix))
          }

          // Parse members
          members.sortBy(key).flatMap { entry =>
            listElement match {
              case _: Obj   => parse(entry.value.as[YMap])
              case Type.Any => Some(typedValue(entry.value, ctx.graphContext))
              case _ =>
                try { Some(str(value(listElement, entry.value))) } catch {
                  case _: Exception => None
                }
            }
          }
        case None => Seq.empty // Error already handled by contentOfNode
      }
    }

    private def parse(map: YMap): Option[AmfObject] = {
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

    private def parseNodeFields(node: YMap,
                                fields: Seq[Field],
                                sources: SourceMap,
                                transformedId: String,
                                instance: AmfObject) = {
      instance.withId(transformedId)
      traverseFields(node, fields, instance, sources)
      checkLinkables(instance)

      // parsing custom extensions
      instance match {
        case l: DomainElement with Linkable =>
          parseLinkableProperties(node, l)
        case obj: ObjectNode =>
          parseObjectNodeProperties(obj, node, fields)
        case _ => // ignore
      }

      instance match {
        case elm: DomainElement => parseCustomProperties(node, elm)
        case _                  => // ignore
      }

      instance match {
        case ex: ExternalDomainElement
            if unresolvedExtReferencesMap.contains(ex.id) => // check if other node requested this external reference
          unresolvedExtReferencesMap.get(ex.id).foreach { element =>
            ex.raw
              .option()
              .foreach(element.set(ExternalSourceElementModel.Raw, _))
          }
        case _ => // ignore
      }

      nodes = nodes + (transformedId -> instance)
      Some(instance)
    }

    private def traverseFields(map: YMap, fields: Seq[Field], instance: AmfObject, sources: SourceMap): Unit = {
      fields.foreach(f => {
        val k = compactUriFromContext(f.value.iri()) // we are assuming compact uris, we shouldn't!
        map.key(k) match {
          case Some(entry) =>
            traverse(instance, f, value(f.`type`, entry.value), sources, k)
          case _ => // Ignore
        }
      })
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

    private def checkLinkables(instance: AmfObject): Unit = {
      instance match {
        case link: DomainElement with Linkable =>
          referencesMap += (link.id -> link)
          unresolvedReferences.getOrElse(link.id, Nil).foreach {
            case unresolved: Linkable =>
              unresolved.withLinkTarget(link)
            case unresolved: LinkNode =>
              unresolved.withLinkedDomainElement(link)
            case _ =>
              ctx.eh.violation(NotLinkable, instance.id, "Only linkable elements can be linked", instance.annotations)
          }
          unresolvedReferences.update(link.id, Nil)
        case _ => // ignore
      }

      instance match {
        case ref: ExternalSourceElement =>
          unresolvedExtReferencesMap += (ref.referenceId.value -> ref) // process when parse the references node
        case _ => // ignore
      }
    }

    private def setLinkTarget(instance: DomainElement with Linkable, targetId: String) = {
      referencesMap.get(targetId) match {
        case Some(target) => instance.withLinkTarget(target)
        case None =>
          val unresolved: Seq[DomainElement] =
            unresolvedReferences.getOrElse(targetId, Nil)
          unresolvedReferences += (targetId -> (unresolved ++ Seq(instance)))
      }
    }

    private def parseLinkableProperties(map: YMap, instance: DomainElement with Linkable): Unit = {
      val targetIdFieldIri = LinkableElementModel.TargetId.value.iri()
      map
        .key(compactUriFromContext(targetIdFieldIri))
        .flatMap(entry => {
          entry.value.tagType match {
            case YType.Map => retrieveId(entry.value.as[YMap], ctx)
            case YType.Seq => retrieveId(entry.value.as[Seq[YMap]].head, ctx)
            case _ =>
              ctx.eh.violation(UnableToParseDocument,
                               entry.value,
                               s"$targetIdFieldIri field must have a map or array value")
              None
          }
        })
        .foreach { targetId =>
          val transformedId = transformIdFromContext(targetId)
          setLinkTarget(instance, transformedId)
        }

      map
        .key(compactUriFromContext(LinkableElementModel.Label.value.iri()))
        .flatMap(entry => {
          entry.value
            .toOption[Seq[YNode]]
            .flatMap(nodes => nodes.head.toOption[YMap])
            .flatMap(map => map.key(JsonLdKeywords.Value))
            .flatMap(_.value.toOption[YScalar].map(_.text))
        })
        .foreach(s => instance.withLinkLabel(s))
    }

    private def parseCustomProperties(map: YMap, instance: DomainElement): Unit = {
      val properties = map
        .key(compactUriFromContext(DomainElementModel.CustomDomainProperties.value.iri()))
        .map(_.value.as[Seq[YNode]].map(value(Iri, _).as[YScalar].text))
        .getOrElse(Nil)

      val extensions = properties
        .flatMap { uri =>
          map
            .key(transformIdFromContext(uri)) // See ADR adrs/0006-custom-domain-properties-json-ld-rendering.md last consequence item
            .map(entry => {
              val extension  = DomainExtension()
              val entryValue = entry.value
              val obj        = contentOfNode(entryValue).getOrElse(entryValue.as[YMap])

              parseScalarProperty(obj, DomainExtensionModel.Name)
                .map(s => extension.set(DomainExtensionModel.Name, s))
              parseScalarProperty(obj, DomainExtensionModel.Element)
                .map(extension.withElement)

              val definition = CustomDomainProperty()
              definition.id = transformIdFromContext(uri)
              extension.withDefinedBy(definition)

              parse(obj).collect({ case d: DataNode => d }).foreach { pn =>
                extension.withId(pn.id)
                extension.withExtension(pn)
              }

              val sources = retrieveSources(map)
              extension.annotations ++= annotations(nodes, sources, extension.id)

              extension
            })
        }

      if (extensions.nonEmpty) {
        extensions.partition(_.isScalarExtension) match {
          case (scalars, objects) =>
            instance.withCustomDomainProperties(objects)
            applyScalarDomainProperties(instance, scalars)
        }
      }
    }

    private def applyScalarDomainProperties(instance: DomainElement, scalars: Seq[DomainExtension]): Unit = {
      scalars.foreach { e =>
        instance.fields
          .fieldsMeta()
          .find(f => e.element.is(f.value.iri()))
          .foreach(f => {
            instance.fields.entry(f).foreach {
              case FieldEntry(_, value) =>
                value.value.annotations += DomainExtensionAnnotation(e)
            }
          })
      }
    }

    private def parseObjectNodeProperties(obj: ObjectNode, map: YMap, fields: Seq[Field]): Unit = {
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
        if (!ignoredFields
              .contains(fieldUri) && !isAlreadyParsed) { // we do this to prevent parsing name of annotations
          parse(fieldValue.as[YMap]).collect {
            case d: amf.core.client.scala.model.domain.DataNode => obj.addProperty(fieldUri, d)
          }
        }
      }
    }

    private def isReferenceNode(m: YMap): Boolean = m.entries.size == 1 && m.key(JsonLdKeywords.Id).isDefined

    private def traverse(instance: AmfObject, f: Field, node: YNode, sources: SourceMap, key: String): AmfObject = {
      if (assertFieldTypeWithContext(f)(ctx)) {
        doTraverse(instance, f, node, sources, key)
      } else {
        instance
      }
    }

    private def doTraverse(instance: AmfObject, f: Field, node: YNode, sources: SourceMap, key: String) = {
      f.`type` match {
        case _: Obj =>
          parse(node.as[YMap]).foreach(n => instance.setWithoutId(f, n, annotations(nodes, sources, key)))
          instance
        case Iri =>
          instance.setWithoutId(f, iri(node, f), annotations(nodes, sources, key))
        case Str | RegExp | LiteralUri =>
          instance.setWithoutId(f, str(node), annotations(nodes, sources, key))
        case Bool =>
          instance.setWithoutId(f, bool(node), annotations(nodes, sources, key))
        case Type.Int =>
          instance.setWithoutId(f, int(node), annotations(nodes, sources, key))
        case Type.Float =>
          instance.setWithoutId(f, double(node), annotations(nodes, sources, key))
        case Type.Double =>
          instance.setWithoutId(f, double(node), annotations(nodes, sources, key))
        case Type.DateTime =>
          instance.setWithoutId(f, date(node), annotations(nodes, sources, key))
        case Type.Date =>
          instance.setWithoutId(f, date(node), annotations(nodes, sources, key))
        case Type.Any =>
          instance.setWithoutId(f, any(node), annotations(nodes, sources, key))
        case l: SortedArray =>
          val parsed = parseSortedArray(l.element, node.as[YMap])
          instance.setArrayWithoutId(f, parsed, annotations(nodes, sources, key))
        case a: Array =>
          (node.tagType, a.element) match {
            case (YType.Seq, _) =>
              val rawItems = node.as[Seq[YNode]]
              val values: Seq[AmfElement] = a.element match {
                case _: Obj    => rawItems.flatMap(n => parse(n.as[YMap]))
                case Str | Iri => rawItems.map(n => str(value(a.element, n)))
              }
              instance.setArrayWithoutId(f, values, annotations(nodes, sources, key))
            case (YType.Map, _: Obj) =>
              val rawHead = node.as[YMap]
              parse(rawHead) match {
                case Some(head) => instance.setArrayWithoutId(f, Seq(head), annotations(nodes, sources, key))
                case None       => instance // Ignore
              }
            case (YType.Str, Str | Iri) =>
              instance.setArrayWithoutId(f, Seq(str(node)), annotations(nodes, sources, key))
            case _ => instance
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
    private def parseScalarProperty(definition: YMap, field: Field) =
      definition
        .key(compactUriFromContext(field.value.iri()))
        .map(entry => value(field.`type`, entry.value).as[YScalar].text)

    private def findType(typeString: String): Option[ModelDefaultBuilder] =
      ctx.config.registryContext.findType(typeString)

    private def buildType(modelType: ModelDefaultBuilder, ann: Annotations): AmfObject = {
      val instance = modelType.modelInstance
      instance.annotations ++= ann
      instance
    }
  }
}

object FlattenedUnitGraphParser extends GraphContextHelper with GraphParserHelpers {

  def apply(config: ParseConfiguration, aliases: Map[String, String] = Map.empty): FlattenedUnitGraphParser = {
    new FlattenedUnitGraphParser(aliases)(new GraphParserContext(config = config))
  }
  implicit val ctx: GraphParserContext = new GraphParserContext(
      config = LimitedParseConfig(IgnoringErrorHandler)
  )

  /**
    * Returns true if `document` contains a @graph node which in turn must contain a Root node.
    * Root nodes are nodes with @type http://a.ml/vocabularies/document#Unit (BaseUnits) with the property
    * http://a.ml/vocabularies/document#root set to true.
    * @param document document to perform the canParse test on
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

  private[amf] def processGraph(m: YMap, ctx: GraphParserContext, aliases: Map[String, String]) = {
    m.key(JsonLdKeywords.Context).foreach(entry => JsonLdGraphContextParser(entry.value, ctx).parse())
    ctx.addTerms(aliases)
    m.key(JsonLdKeywords.Graph).flatMap { graphEntry =>
      val graphYSeq = graphEntry.value.as[YSequence]
      graphYSeq.nodes.find { node =>
        isRootNode(node)(ctx)
      }
    }
  }
}
