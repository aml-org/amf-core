package amf.core.rdf

import amf.core.annotations.DomainExtensionAnnotation
import amf.core.metamodel.Type.{Any, Array, Iri, Scalar, SortedArray, Str}
import amf.core.metamodel.document.{BaseUnitModel, DocumentModel, SourceMapModel}
import amf.core.metamodel.domain._
import amf.core.metamodel.domain.extensions.DomainExtensionModel
import amf.core.metamodel.{Field, Obj, Type}
import amf.core.model.document._
import amf.core.model.domain
import amf.core.model.domain._
import amf.core.model.domain.extensions.{CustomDomainProperty, DomainExtension}
import amf.core.parser.{Annotations, FieldEntry, ParserContext}
import amf.core.rdf.converter.{AnyTypeConverter, ScalarTypeConverter, StringIriUriRegexParser}
import amf.core.rdf.graph.NodeFinder
import amf.core.rdf.parsers.{DynamicLiteralParser, SourceNodeParser}
import amf.core.vocabulary.Namespace
import amf.plugins.document.graph.parser.GraphParserHelpers
import amf.plugins.features.validation.CoreValidations.{UnableToParseNode, UnableToParseRdfDocument}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class RdfModelParser()(implicit val ctx: ParserContext) extends GraphParserHelpers{

  private val unresolvedReferences       = mutable.Map[String, Seq[DomainElement]]()
  private val unresolvedExtReferencesMap = mutable.Map[String, ExternalSourceElement]()

  private val referencesMap = mutable.Map[String, DomainElement]()

  private val collected: ListBuffer[Annotation] = ListBuffer()

  private var nodes: Map[String, AmfElement] = Map()
  private var graph: Option[RdfModel]        = None
  private val sorter = new DefaultNodeClassSorter()

  def parse(model: RdfModel, location: String): BaseUnit = {
    graph = Some(model)

    val unit = model.findNode(location) match {
      case Some(rootNode) =>
        parse(rootNode, findBaseUnit = true) match {
          case Some(unit: BaseUnit) =>
            unit.set(BaseUnitModel.Location, location.split("#").head)
            unit.withRunNumber(ctx.parserRun)
            unit
          case _ =>
            ctx.eh.violation(UnableToParseRdfDocument,
                             location,
                             s"Unable to parse RDF model for location root node: $location")
            Document()
        }
      case _ =>
        ctx.eh.violation(UnableToParseRdfDocument,
                         location,
                         s"Unable to parse RDF model for location root node: $location")
        Document()
    }

    // Resolve annotations after parsing entire graph
    collected.collect({ case r: ResolvableAnnotation => r }) foreach (_.resolve(nodes))

    unit
  }

  def parse(node: Node, findBaseUnit: Boolean = false): Option[AmfObject] = {
    val id = node.subject
    retrieveType(id, node, findBaseUnit) map { model =>
      val sources  = retrieveSources(id, node)
      val instance = buildType(model)(annots(sources, id))
      instance.withId(id)

      checkLinkables(instance)

      // workaround for lazy values in shape
      val modelFields = model match {
        case shapeModel: ShapeModel =>
          shapeModel.fields ++ Seq(
            ShapeModel.CustomShapePropertyDefinitions,
            ShapeModel.CustomShapeProperties
          )
        case _ => model.fields
      }

      modelFields.foreach(f => {
        val k          = f.value.iri()
        val properties = key(node, k)
        traverse(instance, f, properties, sources, k)
      })

      // parsing custom extensions
      instance match {
        case l: DomainElement with Linkable => parseLinkableProperties(node, l)
        case ex: ExternalDomainElement if unresolvedExtReferencesMap.contains(ex.id) =>
          unresolvedExtReferencesMap.get(ex.id).foreach { element =>
            ex.raw.option().foreach(element.set(ExternalSourceElementModel.Raw, _))
          }
          unresolvedExtReferencesMap.remove(ex.id)
        case _ => // ignore
      }
      instance match {
        case elm: DomainElement => parseCustomProperties(node, elm)
        case _                  => // ignore
      }

      nodes = nodes + (id -> instance)
      instance
    }
  }

  protected def key(node: Node, property: String): Seq[PropertyObject] =
    node.getProperties(property).getOrElse(Nil)

  private def parseLinkableProperties(node: Node, instance: DomainElement with Linkable): Unit = {
    node
      .getProperties(LinkableElementModel.TargetId.value.iri())
      .flatMap(entries => {
        entries.headOption match {
          case Some(Uri(id)) => Some(id)
          case _             => None
        }
      })
      .foreach { targetId =>
        setLinkTarget(instance, targetId)
      }

    node
      .getProperties(LinkableElementModel.Label.value.iri())
      .flatMap(entries => {
        entries.headOption match {
          case Some(Literal(v, _)) => Some(v)
          case _                   => None
        }
      })
      .foreach(s => instance.withLinkLabel(s))
  }

  private def setLinkTarget(instance: DomainElement with Linkable, targetId: String) = {
    referencesMap.get(targetId) match {
      case Some(target) => instance.withLinkTarget(target)
      case None =>
        val unresolved: Seq[DomainElement] = unresolvedReferences.getOrElse(targetId, Nil)
        unresolvedReferences += (targetId -> (unresolved ++ Seq(instance)))
    }
  }

  def parseDynamicLiteral(literal: Literal): ScalarNode = new DynamicLiteralParser().parse(literal)

  def parseDynamicType(id: PropertyObject): Option[DataNode] = {
    findLink(id).map { node =>
      val sources = retrieveSources(id.value, node)
      val builder = retrieveDynamicType(node.subject, node).get

      builder(annots(sources, id.value)) match {
        case obj: ObjectNode =>
          obj.withId(node.subject)
          node.getKeys().foreach { uri =>
            if (uri != "@type" && uri != "@id" && uri != DomainElementModel.Sources.value.iri() &&
                uri != (Namespace.Core + "name").iri()) { // we do this to prevent parsing name of annotations

              val dataNode = node.getProperties(uri).get.head match {
                case l @ Literal(_, _) =>
                  parseDynamicLiteral(l)
                case entry if isRDFArray(entry) =>
                  parseDynamicArray(entry)
                case nestedNode @ Uri(_) =>
                  parseDynamicType(nestedNode).getOrElse(ObjectNode())
                case _ => ObjectNode()
              }
              obj.addProperty(uri, dataNode)
            }
          }
          obj

        case scalar: ScalarNode =>
          scalar.withId(node.subject)
          node.getKeys().foreach { k =>
            val entries = node.getProperties(k).get
            if (k == ScalarNodeModel.Value.value.iri() && entries.head.isInstanceOf[Literal]) {
              val parsedScalar = parseDynamicLiteral(entries.head.asInstanceOf[Literal])

              parsedScalar.value.option().foreach { v =>
                scalar.set(ScalarNodeModel.Value, AmfScalar(v, parsedScalar.value.annotations()))
              }
            }
          }
          scalar

        case link: LinkNode =>
          link.withId(node.subject)
          node.getKeys().foreach { k =>
            val entries = node.getProperties(k).get
            if (k == LinkNodeModel.Alias.value.iri() && entries.head.isInstanceOf[Literal]) {
              val parsedScalar = parseDynamicLiteral(entries.head.asInstanceOf[Literal])
              parsedScalar.value.option().foreach(link.withAlias)
            } else if (k == LinkNodeModel.Value.value.iri() && entries.head.isInstanceOf[Literal]) {
              val parsedScalar = parseDynamicLiteral(entries.head.asInstanceOf[Literal])
              parsedScalar.value.option().foreach(link.withLink)
            }
          }
          referencesMap.get(link.alias.value()) match {
            case Some(target) => link.withLinkedDomainElement(target)
            case _ =>
              val unresolved: Seq[DomainElement] = unresolvedReferences.getOrElse(link.alias.value(), Nil)
              unresolvedReferences += (link.alias.value() -> (unresolved ++ Seq(link)))
          }
          link

        case array: ArrayNode =>
          array.withId(node.subject)
          node.getKeys().foreach { k =>
            if (k == ArrayNodeModel.Member.value.iri()) {
              array.withMembers(node.getProperties(k).getOrElse(Nil).flatMap(parseDynamicType))
            }
          }
          array

        case other =>
          throw new Exception(s"Cannot parse object data node from non object JSON structure $other")
      }
    }
  }

  def isRDFArray(entry: PropertyObject): Boolean = {
    entry match {
      case id @ Uri(_) =>
        findLink(id) match {
          case Some(node) =>
            node.getProperties((Namespace.Rdf + "first").iri()).isDefined ||
              node.getProperties((Namespace.Rdf + "rest").iri()).isDefined
          case _ => false
        }
      case _ => false
    }
  }

  def parseDynamicArray(propertyObject: PropertyObject): ArrayNode = {
    val nodeAnnotations = findLink(propertyObject) match {
      case Some(node) =>
        val sources = retrieveSources(node.subject, node)
        annots(sources, node.subject)
      case None => Annotations()
    }
    val nodes = parseDynamicArrayInner(propertyObject)
    val array = ArrayNode(nodeAnnotations)
    nodes.foreach { array.addMember }
    array
  }

  def parseDynamicArrayInner(entry: PropertyObject, acc: Seq[DataNode] = Nil): Seq[DataNode] = {
    findLink(entry) match {
      case Some(n) =>
        val nextNode  = n.getProperties((Namespace.Rdf + "next").iri()).getOrElse(Nil).headOption
        val firstNode = n.getProperties((Namespace.Rdf + "first").iri()).getOrElse(Nil).headOption
        val updatedAcc = firstNode match {
          case Some(id @ Uri(_)) =>
            parseDynamicType(id) match {
              case Some(member) => acc ++ Seq(member)
              case _            => acc
            }
          case _ => acc
        }
        nextNode match {
          case Some(nextNodeProp @ Uri(id)) if id != (Namespace.Rdf + "nil").iri() =>
            parseDynamicArrayInner(nextNodeProp, updatedAcc)
          case _ =>
            updatedAcc
        }
      case None => acc
    }
  }

  private def traverse(instance: AmfObject,
                       f: Field,
                       properties: Seq[PropertyObject],
                       sources: SourceMap,
                       key: String): Unit = {
    if (properties.isEmpty) return
    val property = properties.head
    f.`type` match {
      case DataNodeModel => // dynamic nodes parsed here
        parseDynamicType(property) match {
          case Some(parsed) => instance.set(f, parsed, annots(sources, key))
          case _            =>
        }
      case _: Obj =>
        findLink(property) match {
          case Some(node) =>
            parse(node) match {
              case Some(parsed) =>
                instance.set(f, parsed, annots(sources, key))
              case _ => // ignore
            }
          case _ =>
            ctx.eh.violation(
              UnableToParseRdfDocument,
              instance.id,
              s"Error parsing RDF graph node, unknown linked node for property $key in node ${instance.id}")
        }

      case array: SortedArray if properties.length == 1 => parseList(instance, array, f, properties, annots(sources, key))
      case _: SortedArray =>
        ctx.eh.violation(
          UnableToParseRdfDocument,
          instance.id,
          s"Error, more than one sorted array values found in node for property $key in node ${instance.id}")
      case a: Array =>
        val items = properties
        val values: Seq[AmfElement] = a.element match {
          case _: Obj =>
            val shouldParseUnit = f.value.iri() == (Namespace.Document + "references")
              .iri() // this is for self-encoded documents
            items.flatMap(n =>
              findLink(n) match {
                case Some(o) => parse(o, shouldParseUnit)
                case _       => None
            })
          case Str | Iri => items.map(StringIriUriRegexParser().parse)
        }
        instance.setArrayWithoutId(f, values, annots(sources, key))
      case _: Scalar => parseScalar(instance, f, property, annots(sources, key))
      case Any => parseAny(instance, f, property, annots(sources, key))
    }

  }

  private def parseList(instance: AmfObject, l: SortedArray, field: Field, properties: Seq[PropertyObject], annotations: Annotations): Unit =
    instance.setArray(field, parseList(l.element, findLink(properties.head)), annotations)

  private def parseScalar(instance: AmfObject, field: Field, property: PropertyObject, annotations: Annotations): Unit =
    ScalarTypeConverter(field.`type`, property)(ctx.eh).tryConvert().foreach(instance.set(field, _, annotations))

  private def parseAny(instance: AmfObject, field: Field, property: PropertyObject, annotations: Annotations): Unit =
    AnyTypeConverter(property)(ctx.eh).tryConvert().foreach(instance.set(field, _, annotations))

  private def parseList(listElement: Type, maybeCollection: Option[Node]): Seq[AmfElement] = {
    val buffer = ListBuffer[PropertyObject]()
    maybeCollection.foreach { collection =>
      collection.getKeys().foreach { entry =>
        if (entry.startsWith((Namespace.Rdfs + "_").iri())) {
          buffer ++= collection.getProperties(entry).get
        }
      }
    }

    val res = buffer.map { property =>
      listElement match {
        case DataNodeModel => parseDynamicType(property)
        case _: Obj =>
          findLink(property) match {
            case Some(node) => parse(node)
            case _          => None
          }
        case _: Scalar => converter.ScalarTypeConverter(listElement, property)(ctx.eh).tryConvert()
        case Any       => AnyTypeConverter(property)(ctx.eh).tryConvert()
        case _         => throw new Exception(s"Unknown list element type: $listElement")
      }
    }
    res collect { case Some(x) => x }
  }

  private def findLink(property: PropertyObject) = new NodeFinder(graph.get).findLink(property)

  private def checkLinkables(instance: AmfObject): Unit = {
    instance match {
      case link: DomainElement with Linkable =>
        referencesMap += (link.id -> link)
        unresolvedReferences.getOrElse(link.id, Nil).foreach {
          case unresolved: Linkable =>
            unresolved.withLinkTarget(link)
          case unresolved: LinkNode =>
            unresolved.withLinkedDomainElement(link)
          case _ => throw new Exception("Only linkable elements can be linked")
        }
        unresolvedReferences.update(link.id, Nil)
      case ref: ExternalSourceElement =>
        unresolvedExtReferencesMap += (ref.referenceId.value -> ref) // process when parse the references node
      case _ => // ignore
    }
  }

  private def isUnitModel(typeModel: Obj): Boolean =
    typeModel.isInstanceOf[DocumentModel] || typeModel.isInstanceOf[EncodesModel] || typeModel
      .isInstanceOf[DeclaresModel] || typeModel.isInstanceOf[BaseUnitModel]

  private def retrieveType(id: String, node: Node, findBaseUnit: Boolean = false): Option[Obj] = {
    val types = sorter.sortedClassesOf(node)

    val foundType = types.find { t =>
      val maybeFoundType = findType(t)
      // this is just for self-encoding documents
      maybeFoundType match {
        case Some(typeModel) if !findBaseUnit && !isUnitModel(typeModel) => true
        case Some(typeModel) if findBaseUnit && isUnitModel(typeModel)   => true
        case _                                                           => false
      }
    } orElse {
      // if I cannot find it, I will return the matching one directly, this is used
      // in situations where the references a reified, for example, in the canonical web api spec
      types.find(findType(_).isDefined)
    }

    foundType match {
      case Some(t) => findType(t)
      case None =>
        ctx.eh.violation(UnableToParseNode,
                         id,
                         s"Error parsing JSON-LD node, unknown @types $types",
                         ctx.rootContextDocument)
        None
    }
  }

  def retrieveDynamicType(id: String, node: Node): Option[Annotations => AmfObject] = {
    sorter.sortedClassesOf(node).find({ t =>
      dynamicBuilders.contains(t)
    }) match {
      case Some(t) => Some(dynamicBuilders(t))
      case _       => None
    }
  }

  private def findType(`type`: String): Option[Obj] = ctx.plugins.findType(`type`)

  private def buildType(`type`: Obj): Annotations => AmfObject = ctx.plugins.buildType(`type`)
  private val dynamicBuilders: mutable.Map[String, Annotations => AmfObject] = mutable.Map(
    LinkNode.builderType.iri()        -> domain.LinkNode.apply,
    ArrayNode.builderType.iri()       -> domain.ArrayNode.apply,
    ScalarNodeModel.`type`.head.iri() -> domain.ScalarNode.apply,
    ObjectNode.builderType.iri()      -> domain.ObjectNode.apply
  )

  protected def retrieveSources(id: String, node: Node): SourceMap = {
    node
      .getProperties(DomainElementModel.Sources.value.iri())
      .flatMap { properties =>
        if (properties.nonEmpty) {
          findLink(properties.head) match {
            case Some(sourceNode) => Some(new SourceNodeParser(new NodeFinder(graph.get)).parse(sourceNode))
            case _                => None
          }
        } else {
          None
        }
      }
      .getOrElse(SourceMap.empty)
  }

  def parseCustomProperties(node: Node, instance: DomainElement): Unit = {
    val properties: Seq[String] = node
      .getProperties(DomainElementModel.CustomDomainProperties.value.iri())
      .getOrElse(Nil)
      .filter(_.isInstanceOf[Uri])
      .map(_.asInstanceOf[Uri].value)

    val extensions: Seq[DomainExtension] = properties.flatMap { uri =>
      node
        .getProperties(uri)
        .map(entries => {
          val extension = DomainExtension()
          if (entries.nonEmpty) {
            findLink(entries.head) match {
              case Some(obj) =>
                obj.getProperties(DomainExtensionModel.Name.value.iri()) match {
                  case Some(es) if es.nonEmpty && es.head.isInstanceOf[Literal] =>
                    extension.withName(value(DomainExtensionModel.Name.`type`, es.head.asInstanceOf[Literal].value))
                  case _ => // ignore
                }

                obj.getProperties(DomainExtensionModel.Element.value.iri()) match {
                  case Some(es) if es.nonEmpty && es.head.isInstanceOf[Literal] =>
                    extension.withName(value(DomainExtensionModel.Element.`type`, es.head.asInstanceOf[Literal].value))
                  case _ => // ignore
                }

                val definition = CustomDomainProperty()
                definition.id = uri
                extension.withDefinedBy(definition)

                parseDynamicType(entries.head).foreach { pn =>
                  extension.withId(pn.id)
                  extension.withExtension(pn)
                }

                val sources = retrieveSources(extension.id, node)
                extension.annotations ++= annots(sources, extension.id)

              case _ => // ignore
            }
          }
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
            case FieldEntry(_, value) => value.value.annotations += DomainExtensionAnnotation(e)
          }
        })
    }
  }

  private def annots(sources: SourceMap, key: String) =
    annotations(nodes, sources, key).into(collected, _.isInstanceOf[ResolvableAnnotation])
}

class DefaultNodeClassSorter(){

  private val deferredTypesSet = Set(
    (Namespace.Document + "Document").iri(),
    (Namespace.Document + "Fragment").iri(),
    (Namespace.Document + "Module").iri(),
    (Namespace.Document + "Unit").iri(),
    (Namespace.Shacl + "Shape").iri(),
    (Namespace.Shapes + "Shape").iri()
  )

  def sortedClassesOf(node: Node): Seq[String] = {
    node.classes.partition(deferredTypesSet.contains) match {
      case (deferred, others) => others ++ deferred.sorted // we just use the fact that lexical order is correct
    }
  }
}