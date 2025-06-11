package amf.core.internal.plugins.document.graph.parser

import amf.core.client.scala.model.document.SourceMap
import amf.core.client.scala.model.domain.extensions.{CustomDomainProperty, DomainExtension}
import amf.core.client.scala.model.domain._
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.annotations.DomainExtensionAnnotation
import amf.core.internal.metamodel.Type.Iri
import amf.core.internal.metamodel.domain.{DomainElementModel, ExternalSourceElementModel, LinkableElementModel}
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel
import amf.core.internal.metamodel.{Field, ModelDefaultBuilder, Obj, Type}
import amf.core.internal.parser.{YMapOps, YNodeLikeOps}
import amf.core.internal.parser.domain.{Annotations, FieldEntry}
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import amf.core.internal.validation.CoreValidations.{NotLinkable, UnableToParseDomainElement, UnableToParseNode}
import org.yaml.model._

import scala.collection.mutable

abstract class CommonGraphParser(implicit ctx: GraphParserContext) extends GraphParserHelpers {

  protected var nodes: Map[String, AmfElement] = Map()
  protected val unresolvedReferences: mutable.Map[String, Seq[DomainElement]] =
    mutable.Map[String, Seq[DomainElement]]()
  protected val unresolvedExtReferencesMap: mutable.Map[String, ExternalSourceElement] =
    mutable.Map[String, ExternalSourceElement]()
  protected val referencesMap: mutable.Map[String, DomainElement] = mutable.Map[String, DomainElement]()

  protected def parse(map: YMap): Option[AmfObject]
  protected def parseAtTraversion(node: YNode, `type`: Type): Option[AmfElement]
  protected def parseLinkableProperties(map: YMap, instance: DomainElement with Linkable): Unit
  protected def parseObjectNodeProperties(obj: ObjectNode, map: YMap, fields: Seq[Field]): Unit

  protected def parseNodeFields(
      node: YMap,
      fields: Seq[Field],
      sources: SourceMap,
      transformedId: String,
      instance: AmfObject
  ): Option[AmfObject] = {
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

  protected def checkLinkables(instance: AmfObject): Unit = {
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

  protected def setLinkTarget(instance: DomainElement with Linkable, targetId: String): Unit = {
    referencesMap.get(targetId) match {
      case Some(target) => instance.withLinkTarget(target)
      case None =>
        val unresolved: Seq[DomainElement] =
          unresolvedReferences.getOrElse(targetId, Nil)
        unresolvedReferences += (targetId -> (unresolved ++ Seq(instance)))
    }
  }

  private def traverseFields(
      map: YMap,
      fields: Seq[Field],
      instance: AmfObject,
      sources: SourceMap
  ): Unit = {
    fields.foreach(f => {
      val k = compactUriFromContext(f.value.iri()) // we are assuming compact uris, we shouldn't!
      map.key(k) match {
        case Some(entry) =>
          traverse(instance, f, value(f.`type`, entry.value), sources, k)
        case _ => // Ignore
      }
    })
  }

  private def traverse(
      instance: AmfObject,
      f: Field,
      node: YNode,
      sources: SourceMap,
      key: String
  ): AmfObject = {
    if (assertFieldTypeWithContext(f)(ctx)) {
      doTraverse(instance, f, node, sources, key)
    } else instance
  }

  private def doTraverse(
      instance: AmfObject,
      f: Field,
      node: YNode,
      sources: SourceMap,
      key: String
  ): AmfObject = {
    parseAtTraversion(node, f.`type`).foreach(r => instance.setWithoutId(f, r, annotations(nodes, sources, key)))
    instance
  }

  protected def mapLinkableProperties(map: YMap, instance: DomainElement with Linkable): Unit = {
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

  protected def findType(typeIris: Seq[String], id: String, map: YMap): Option[ModelDefaultBuilder] = {
    typeIris.find(findType(_).isDefined) match {
      case Some(t) => findType(t)
      case None =>
        ctx.eh
          .violation(UnableToParseNode, id, s"Error parsing JSON-LD node, unknown @types $typeIris", map.location)
        None
    }
  }

  protected def applyScalarDomainProperties(instance: DomainElement, scalars: Seq[DomainExtension]): Unit = {
    scalars.foreach { e =>
      instance.fields
        .fieldsMeta()
        .find(f => e.element.is(f.value.iri()))
        .foreach(f => {
          instance.fields.entry(f).foreach { case FieldEntry(_, value) =>
            value.value.annotations += DomainExtensionAnnotation(e)
          }
        })
    }
  }

  protected def yNodeSeq(
      node: YNode,
      a: Type.Array
  ): Option[AmfArray] = {
    val rawItems = node.as[Seq[YNode]]
    val values = rawItems.flatMap { ri =>
      parseAtTraversion(value(a.element, ri), a.element)
    }
    Some(AmfArray(values))
  }

  private def parseCustomProperties(map: YMap, instance: DomainElement): Unit = {
    // See ADR adrs/0006-custom-domain-properties-json-ld-rendering.md last consequence item
    val extensions: Seq[DomainExtension] = for {
      uri       <- customDomainPropertiesFor(map)
      entry     <- asSeq(map.key(transformIdFromContext(uri)))
      extension <- parseCustomDomainPropertyEntry(uri, entry)
    } yield {
      extension
    }
    if (extensions.nonEmpty) {
      extensions.partition(_.isScalarExtension) match {
        case (scalars, objects) =>
          instance.withCustomDomainProperties(objects)
          applyScalarDomainProperties(instance, scalars)
      }
    }
  }

  private def customDomainPropertiesFor(map: YMap): Seq[String] = {
    val fieldIri   = DomainElementModel.CustomDomainProperties.value.iri()
    val compactIri = compactUriFromContext(fieldIri)

    map.key(compactIri) match {
      case Some(entry) =>
        for {
          valueNode <- entry.value.as[Seq[YNode]]
        } yield {
          value(Iri, valueNode).as[YScalar].text
        }
      case _ =>
        Nil
    }
  }

  private def parseCustomDomainPropertyEntry(
      uri: String,
      entry: YMapEntry
  ): Seq[DomainExtension] = {
    entry.value.tagType match {
      case YType.Map =>
        Seq(parseSingleDomainExtension(entry.value.as[YMap], uri))
      case YType.Seq =>
        val values = entry.value.as[YSequence]
        values.nodes.map { value =>
          parseSingleDomainExtension(value.as[YMap], uri)
        }
      case _ =>
        ctx.eh
          .violation(UnableToParseDomainElement, uri, s"Cannot parse domain extensions for '$uri'", entry.location)
        Nil
    }
  }

  private def parseSingleDomainExtension(
      map: YMap,
      uri: String
  ): DomainExtension = {
    val extension = DomainExtension()
    contentOfNode(map) match {
      case Some(obj) =>
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

        val sources = retrieveSources(obj)
        extension.annotations ++= annotations(nodes, sources, extension.id)
      case None =>
        val nodeId = s"${retrieveId(map, ctx)}"
        ctx.eh.violation(
          UnableToParseDomainElement,
          nodeId,
          s"Cannot find node definition for node '$nodeId'",
          map.location
        )
    }
    extension
  }

  private def parseScalarProperty(definition: YMap, field: Field): Option[String] =
    definition
      .key(compactUriFromContext(field.value.iri()))
      .map(entry => value(field.`type`, entry.value).as[YScalar].text)

  protected def annotations(nodes: Map[String, AmfElement], sources: SourceMap, key: String): Annotations =
    ctx.config.serializableAnnotationsFacade.retrieveAnnotation(nodes, sources, key)

  private def findType(typeString: String): Option[ModelDefaultBuilder] = {
    ctx.config.registryContext.findType(expandUriFromContext(typeString))
  }

  protected def parseListMembers(members: List[YNode], listType: Type): Seq[AmfElement] = {
    val fn     = parseListMember(listType)
    val result = members.flatMap(fn(_))
    result
  }

  private def parseListMember(listType: Type): YNode => Option[AmfElement] = (member: YNode) => {
    listType match {
      case _: Obj =>
        parse(member.as[YMap])
      case Type.Any =>
        Some(typedValue(member, ctx.graphContext))
      case _ =>
        try {
          Some(str(value(listType, member)))
        } catch {
          case _: Exception => None
        }
    }
  }

  private def getEntryKeyString(entry: YMapEntry): String = entry.key.as[String]

  protected def getArrayMapMembers(node: YMap): List[YNode] = {
    node.entries
      .filter { entry =>
        val property           = getEntryKeyString(entry)
        val sortedMemberPrefix = (Namespace.Rdfs + "_").iri()
        property.startsWith(compactUriFromContext(sortedMemberPrefix))
      }
      .sortBy(getEntryKeyString)
      .map { entry =>
        entry.value.tagType match {
          case YType.Seq =>
            entry.value.as[Seq[YNode]].head
          case _ =>
            entry.value
        }
      }
      .toList
  }

  protected def getListMembers(node: YMapEntry): List[YNode] = {
    node.value.value match {
      case sequence: YSequence => sequence.nodes.toList
      case _                   => List.empty
    }
  }
}
