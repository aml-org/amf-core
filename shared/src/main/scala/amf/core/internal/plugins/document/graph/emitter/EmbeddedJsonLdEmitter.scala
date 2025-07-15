package amf.core.internal.plugins.document.graph.emitter

import amf.core.client.scala.config
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.{BaseUnit, SourceMap}
import amf.core.client.scala.model.domain._
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.vocabulary.{Namespace, NamespaceAliases}
import amf.core.internal.metamodel._
import amf.core.internal.metamodel.document.{ModuleModel, SourceMapModel}
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel
import amf.core.internal.parser.domain.{FieldEntry, Value}
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import org.yaml.builder.DocBuilder
import org.yaml.builder.DocBuilder.{Entry, Part, SType, Scalar}

import scala.collection.mutable

// TODO: Should be erased. Left for backwards compatibility in AMF Tests
private[amf] object EmbeddedJsonLdEmitter {

  def emit[T](
      unit: BaseUnit,
      builder: DocBuilder[T],
      renderOptions: RenderOptions = config.RenderOptions(),
      namespaceAliases: NamespaceAliases = Namespace.defaultAliases
  ): Boolean = {
    implicit val ctx: GraphEmitterContext =
      GraphEmitterContext(unit, renderOptions, namespaceAliases = namespaceAliases)
    new EmbeddedJsonLdEmitter[T](builder, renderOptions, InstanceMetaApplicableFieldRenderProvider).root(unit)
    true
  }
}

// TODO: Should be erased. Left for backwards compatibility in AMF Tests
private[amf] class EmbeddedJsonLdEmitter[T] private (
    val builder: DocBuilder[T],
    val options: RenderOptions,
    val fieldProvision: ApplicableMetaFieldRenderProvider
)(implicit ctx: GraphEmitterContext)
    extends CommonEmitter[T, GraphEmitterContext](options)
    with MetaModelTypeMapping {

  val cache: mutable.Map[String, T] = mutable.Map[String, T]()

  def root(unit: BaseUnit): Unit = {
    val (declaresEntry: Option[FieldEntry], referencesEntry: Option[FieldEntry]) = extractDeclarationsAndReferences(
      unit
    )

    builder.list {
      _.obj { eb =>
        traverse(unit, eb)
        emitDeclarationsAndReferences(unit, eb)
        ctx.emitContext(eb)
      }
    }

    // Restore model previous version
    declaresEntry.foreach(e => unit.fields.setWithoutId(ModuleModel.Declares, e.array))
    referencesEntry.foreach(e => unit.fields.setWithoutId(ModuleModel.References, e.array))
  }

  def traverse(element: AmfObject, b: Entry[T]): Unit = {
    val id = element.id

    createIdNode(b, id)

    val sources = SourceMap(id, element)

    val obj = metaModel(element)
    traverseMetaModel(id, element, sources, obj, b)

    emitDomainExtensions(element, b)

    val sourceMapId = sourceMapIdFor(id)
    createSourcesNode(sourceMapId, sources, b)
  }

  def traverseMetaModel(id: String, element: AmfObject, sources: SourceMap, obj: Obj, b: Entry[T]): Unit = {
    createTypeNode(b, obj)
    val modelFields = fieldProvision.fieldsFor(element, options)
    modelFields.foreach { f =>
      emitStaticField(f, element, id, sources, b)
    }
  }

  override protected def createCustomExtensionNode(
      b: Entry[T],
      uri: String,
      extension: DomainExtension,
      field: Option[Field] = None
  ): Unit = {
    b.entry(
      ctx.emitIri(DomainExtensionModel.Name.value.iri()),
      emitScalar(_, extension.name.value())
    )
    field.foreach(f =>
      b.entry(
        ctx.emitIri(DomainExtensionModel.Element.value.iri()),
        emitScalar(_, f.value.iri())
      )
    )
    traverse(extension.extension, b)
  }

  override protected def createSortedArray(
      a: Type,
      v: Value,
      b: Part[T],
      parent: String,
      sources: Value => Unit
  ): Unit = {
    val seq = v.value.asInstanceOf[AmfArray].values
    sources(v)
    b.list {
      _.obj { b =>
        val id = s"$parent/list"
        createIdNode(b, id)
        b.entry(JsonLdKeywords.Type, ctx.emitIri((Namespace.Rdfs + "Seq").iri()))
        seq.zipWithIndex.foreach { case (e, i) =>
          b.entry(
            ctx.emitIri((Namespace.Rdfs + s"_${i + 1}").iri()),
            { b =>
              b.list { b =>
                emitArrayMember(a, e, b)
              }
            }
          )
        }
      }
    }
  }

  override protected def emitArrayObjectMember(b: Part[T], member: AmfObject): Unit = obj(b, member, inArray = true)

  override protected def obj(b: Part[T], element: AmfObject, inArray: Boolean = false): Unit = {
    def emit(b: Part[T]): Unit = {
      cache.get(element.id) match {
        case Some(value) => b.+=(value)
        case None if isExternalLink(element) =>
          b.obj(traverse(element, _)) // don't add references to the cache, duplicated IDs
        case None => b.obj(traverse(element, _)).foreach(cache.put(element.id, _))
      }
    }

    if (inArray) emit(b) else b.list(emit)
  }

  override protected def emitAmfObject(amfObject: AmfObject, entry: Entry[T]): Unit = {
    traverse(amfObject, entry)
  }

  private def isExternalLink(element: AmfObject) =
    element.isInstanceOf[DomainElement] && element.asInstanceOf[DomainElement].isExternalLink.option().getOrElse(false)

  override protected def emitAnnotations(id: String, filteredSources: SourceMap, b: Entry[T]): Unit = {
    createIdNode(b, id)
    createTypeNode(b, SourceMapModel)
    createAnnotationNodes(id, b, filteredSources.annotations)
    createAnnotationNodes(id, b, filteredSources.eternals)
  }

  override protected def emitEternalNode(id: String, sources: SourceMap, b: Entry[T]): Unit = {
    createIdNode(b, id)
    createTypeNode(b, SourceMapModel)
    createAnnotationNodes(id, b, sources.eternals)
  }

  override protected def createAnnotationValueNode(id: String, b: Part[T], annotationEntry: (String, String)): Unit =
    annotationEntry match {
      case (iri, v) => withAnnotationEntry(id, b, iri, v)
    }

  override protected def scalar(b: Part[T], content: String, t: SType): Unit =
    b.obj(_.entry(JsonLdKeywords.Value, Scalar(t, content)))

  override protected def emitScalar(b: Part[T], content: String, t: SType): Unit = b.list(scalar(_, content, t))

  override protected def emitScalar(b: Part[T], content: AmfElement, t: SType): Unit = b.list(scalar(_, content, t))

}
