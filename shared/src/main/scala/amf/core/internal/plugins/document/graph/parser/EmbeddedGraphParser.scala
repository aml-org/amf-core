package amf.core.internal.plugins.document.graph.parser

import amf.core.internal.annotations.DomainExtensionAnnotation
import amf.core.internal.metamodel.Type.{Array, Bool, Iri, LiteralUri, RegExp, SortedArray, Str}
import amf.core.internal.metamodel._
import amf.core.internal.metamodel.document.BaseUnitModel.Location
import amf.core.internal.metamodel.domain._
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel
import amf.core.client.scala.model.document._
import amf.core.client.scala.model.domain._
import amf.core.client.scala.model.domain.extensions.{CustomDomainProperty, DomainExtension}
import amf.core.internal.parser._
import amf.core.internal.parser.domain.FieldEntry
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.CompilerConfiguration
import amf.core.internal.parser.domain.{Annotations, FieldEntry}
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import amf.core.internal.plugins.syntax.SyamlAMFErrorHandler
import amf.core.internal.validation.CoreValidations.{NotLinkable, UnableToParseDocument, UnableToParseNode}
import org.yaml.convert.YRead.SeqNodeYRead
import org.yaml.model._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * AMF Graph parser
  */
class EmbeddedGraphParser(private val aliases: Map[String, String])(implicit val ctx: GraphParserContext)
    extends GraphParserHelpers {

  def canParse(document: SyamlParsedDocument): Boolean = EmbeddedGraphParser.canParse(document)

  def parse(document: YDocument, location: String): BaseUnit = {
    val parser = Parser(Map())
    parser.parse(document, location)
  }

  def annotations(nodes: Map[String, AmfElement], sources: SourceMap, key: String): Annotations =
    ctx.config.serializableAnnotationsFacade.retrieveAnnotation(nodes, sources, key)

  case class Parser(var nodes: Map[String, AmfElement]) {
    private val unresolvedReferences = mutable.Map[String, Seq[DomainElement]]()
    private val unresolvedExtReferencesMap =
      mutable.Map[String, ExternalSourceElement]()

    private val referencesMap = mutable.Map[String, DomainElement]()

    def parse(document: YDocument, location: String): BaseUnit = {
      val parsedOption = for {
        seq  <- document.node.toOption[Seq[YMap]]
        head <- seq.headOption
        parsed <- {
          head.key(JsonLdKeywords.Context, e => JsonLdGraphContextParser(e.value, ctx).parse())
          aliases.foreach {
            case (term, iri) => ctx.graphContext.withTerm(term, iri)
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

    private def retrieveType(id: String, map: YMap): Option[ModelDefaultBuilder] = {
      val stringTypes = ts(map, id)
      stringTypes.find(findType(_).isDefined) match {
        case Some(t) => findType(t)
        case None =>
          ctx.eh
            .violation(UnableToParseNode, id, s"Error parsing JSON-LD node, unknown @types $stringTypes", map.location)
          None
      }
    }

    private def parseList(listElement: Type, node: YMap): Seq[AmfElement] = {
      val buffer = ListBuffer[YNode]()
      node.entries.sortBy(_.key.as[String]).foreach { entry =>
        if (entry.key.as[String].startsWith(compactUriFromContext((Namespace.Rdfs + "_").iri()))) {
          buffer += entry.value.as[Seq[YNode]].head
        }
      }
      buffer.flatMap { n =>
        listElement match {
          case _: Obj   => parse(n.as[YMap])
          case Type.Any => Some(typedValue(n, ctx.graphContext))
          case _ =>
            try { Some(str(value(listElement, n))) } catch {
              case _: Exception => None
            }
        }
      }
    }

    private def parse(map: YMap): Option[AmfObject] = {
      retrieveId(map, ctx)
        .flatMap(value => retrieveType(value, map).map(value2 => (value, value2)))
        .flatMap {
          case (id, model) =>
            val sources               = retrieveSources(map)
            val transformedId: String = transformIdFromContext(id)

            val instance: AmfObject = buildType(model, annotations(nodes, sources, transformedId))
            instance.withId(transformedId)

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
              val k = compactUriFromContext(f.value.iri())
              map.key(k) match {
                case Some(entry) =>
                  traverse(instance, f, value(f.`type`, entry.value), sources, k)
                case _ =>
              }
            })

            checkLinkables(instance)

            // parsing custom extensions
            instance match {
              case l: DomainElement with Linkable =>
                parseLinkableProperties(map, l)
              case obj: ObjectNode =>
                parseObjectNodeProperties(obj, map, modelFields)

              case _ => // ignore
            }

            instance match {
              case ex: ExternalDomainElement
                  if unresolvedExtReferencesMap
                    .contains(ex.id) => // check if other node requested this external reference
                unresolvedExtReferencesMap.get(ex.id).foreach { element =>
                  ex.raw
                    .option()
                    .foreach(element.set(ExternalSourceElementModel.Raw, _))
                }
              case _ => // ignore
            }

            instance match {
              case elm: DomainElement => parseCustomProperties(map, elm)
              case _                  => // ignore
            }

            nodes = nodes + (transformedId -> instance)
            Some(instance)
          case _ => None
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
      map
        .key(compactUriFromContext(LinkableElementModel.TargetId.value.iri()))
        .flatMap(entry => {
          retrieveId(entry.value.as[Seq[YMap]].head, ctx)
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
              val extension = DomainExtension()
              val obj       = entry.value.as[YMap]

              parseScalarProperty(obj, DomainExtensionModel.Name)
                .map(extension.set(DomainExtensionModel.Name, _))
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

    private def parseObjectNodeProperties(obj: ObjectNode, map: YMap, fields: List[Field]): Unit = {
      map.entries.foreach { entry =>
        val uri = expandUriFromContext(entry.key.as[String])
        val v   = entry.value
        if (uri != JsonLdKeywords.Type && uri != JsonLdKeywords.Id && uri != DomainElementModel.Sources.value
              .iri() && uri != "smaps" &&
            uri != (Namespace.Core + "extensionName").iri() && !fields
              .exists(_.value.iri() == uri)) { // we do this to prevent parsing name of annotations
          v.as[Seq[YMap]]
            .headOption
            .flatMap(parse)
            .collect({ case d: amf.core.client.scala.model.domain.DataNode => obj.addProperty(uri, d) })
        }
      }
    }

    private def traverse(instance: AmfObject, f: Field, node: YNode, sources: SourceMap, key: String) = {
      if (assertFieldTypeWithContext(f)(ctx)) {
        doTraverse(instance, f, node, sources, key)
      } else instance
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
          instance.setArrayWithoutId(f, parseList(l.element, node.as[YMap]), annotations(nodes, sources, key))
        case a: Array =>
          val items = node.as[Seq[YNode]]
          val values: Seq[AmfElement] = a.element match {
            case _: Obj    => items.flatMap(n => parse(n.as[YMap]))
            case Str | Iri => items.map(n => str(value(a.element, n)))
          }
          instance.setArrayWithoutId(f, values, annotations(nodes, sources, key))
      }
    }
  }

  private def parseScalarProperty(definition: YMap, field: Field) =
    definition
      .key(compactUriFromContext(field.value.iri()))
      .map(entry => value(field.`type`, entry.value).as[YScalar].text)

  private def findType(typeString: String): Option[ModelDefaultBuilder] = {
    ctx.config.registryContext.findType(expandUriFromContext(typeString))
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
        val types                                 = Seq("Document", "Fragment", "Module", "Unit").map(toDocumentNamespace)

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
