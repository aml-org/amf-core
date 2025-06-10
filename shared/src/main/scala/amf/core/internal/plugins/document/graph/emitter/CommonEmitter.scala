package amf.core.internal.plugins.document.graph.emitter

import amf.core.client.scala.config
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.document.SourceMap
import amf.core.client.scala.model.domain.DataNodeOps.adoptTree
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.model.domain._
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.annotations._
import amf.core.internal.metamodel.Type.{Any, Array, Bool, EncodedIri, Iri, LiteralUri, SortedArray, Str}
import amf.core.internal.metamodel.document.ModuleModel
import amf.core.internal.metamodel.domain.{DomainElementModel, LinkableElementModel, ShapeModel}
import amf.core.internal.metamodel.{Field, Obj, Type}
import amf.core.internal.parser.domain.{Annotations, FieldEntry, Value}
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import amf.core.internal.plugins.document.graph.emitter.utils.{ScalarEmitter, SourceMapEmitter, SourceMapsAllowList}
import org.mulesoft.common.collections._
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.builder.DocBuilder.{Entry, Part, SType, Scalar}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class CommonEmitter[T, C <: GraphEmitterContext](options: RenderOptions = config.RenderOptions())(implicit
    ctx: C
) extends SourceMapEmitter
    with ScalarEmitter[T] {

  val allowList: List[String] = SourceMapsAllowList()

  protected def extractDeclarationsAndReferencesToContext(
      declaresEntry: Option[FieldEntry],
      referencesEntry: Option[FieldEntry],
      annotations: Annotations
  ): C = {
    val declaredElements: Iterable[AmfElement] =
      declaresEntry.map(_.value.value.asInstanceOf[AmfArray].values).getOrElse(Nil)
    val referencedElements: Iterable[AmfElement] =
      referencesEntry.map(_.value.value.asInstanceOf[AmfArray].values).getOrElse(Nil)

    val declaredIds   = annotations.find(classOf[Declares]).map(_.declares).getOrElse(Nil)
    val referencedIds = annotations.find(classOf[References]).map(_.references).getOrElse(Nil)
    ctx.registerDeclaredAndReferencedFromAnnotations(declaredIds ++ referencedIds)

    ctx ++ declaredElements
    ctx.addReferences(referencedElements)
  }

  protected def getTypesAsIris(obj: Obj): List[String] = obj.`type`.map(_.iri())

  protected def createTypeNode(b: Entry[T], types: List[String]): Unit = {
    b.entry(
      JsonLdKeywords.Type,
      _.list { b =>
        types.distinct.foreach(t => raw(b, ctx.emitIri(t)))
      }
    )
  }

  protected def createTypeNode(b: Entry[T], obj: Obj): Unit =
    createTypeNode(b, getTypesAsIris(obj))

  protected def raw(b: Part[T], content: String): Unit = b += content

  protected def emitReferences(b: Entry[T], id: String, sources: SourceMap): Unit = {
    if (ctx.referenced.nonEmpty) {
      val v   = Value(AmfArray(ctx.referenced), Annotations())
      val f   = ModuleModel.References
      val url = ctx.emitIri(f.value.iri())
      ctx.emittingReferences(true)
      b.entry(
        url,
        value(f.`type`, v, id, sources.property(url), _)
      )
    }
    ctx.emittingReferences(false)
  }

  // noinspection SameParameterValue
  protected def createAnnotationValueNode(id: String, b: Part[T], annotationEntry: (String, String)): Unit

  protected def createAnnotationNodes(
      id: String,
      b: Entry[T],
      annotations: mutable.ListMap[String, mutable.ListMap[String, String]]
  ): Unit = {
    annotations.foreach({ case (a, values) =>
      if (ctx.options.isWithRawSourceMaps) {
        b.entry(
          a,
          _.obj { o =>
            values.foreach { case (iri, v) =>
              o.entry(
                ctx.emitId(ctx.emitIri(iri)),
                raw(_, v)
              )
            }
          }
        )
      } else {
        b.entry(
          ctx.emitIri(ValueType(Namespace.SourceMaps, a).iri()),
          _.list(b =>
            values.zipWithIndex.foreach { case (annotationEntry, index) =>
              createAnnotationValueNode(s"$id/$a/element_$index", b, annotationEntry)
            }
          )
        )
      }
    })
  }

  protected def filterSourceMaps(sources: SourceMap): SourceMap = {
    val filteredAnnotations = sources.annotations.filter(a => allowList.contains(a._1))
    val filteredEternals    = sources.eternals.filter(a => allowList.contains(a._1))
    new SourceMap(filteredAnnotations, filteredEternals)
  }

  protected def createIdNode(b: Entry[T], id: String): Unit = b.entry(
    JsonLdKeywords.Id,
    raw(_, ctx.emitId(id))
  )

  protected def typedScalar(b: Part[T], content: String, dataType: String, inArray: Boolean = false): Unit = {
    def emit(b: Part[T]): Unit = b.obj { m =>
      m.entry(JsonLdKeywords.Value, raw(_, content))
      m.entry(JsonLdKeywords.Type, raw(_, ctx.emitIri(dataType)))
    }

    if (inArray) emit(b) else b.list(emit)
  }

  protected def iri(b: Part[T], content: String, inArray: Boolean = false): Unit = {
    def emit(b: Part[T]): Unit = {
      b.obj(_.entry(JsonLdKeywords.Id, raw(_, ctx.emitId(content))))
    }

    if (inArray) emit(b) else b.list(emit)
  }

  protected def emitDeclarations(b: Entry[T], id: String, sources: SourceMap): Unit = {
    if (ctx.declared.nonEmpty) {
      val v   = Value(AmfArray(ctx.declared), Annotations())
      val f   = ModuleModel.Declares
      val url = ctx.emitIri(f.value.iri())
      ctx.emittingDeclarations(true)
      b.entry(
        url,
        value(f.`type`, v, id, sources.property(url), _)
      )
    }
    ctx.emittingDeclarations(false)
  }

  protected def emitStaticField(field: Field, element: AmfObject, id: String, sources: SourceMap, b: Entry[T]): Unit = {
    element.fields.entryJsonld(field) match {
      case Some(FieldEntry(f, v)) =>
        val url = ctx.emitIri(f.value.iri())
        b.entry(
          url,
          value(f.`type`, v, id, sources.property(url), _)
        )
      case None => // Missing field
    }
  }

  protected def extractToLink(shape: Shape, b: Part[T], inArray: Boolean = false): Unit = {
    if (!ctx.isDeclared(shape) && !ctx.isInReferencedShapes(shape)) {
      ctx + shape
      shape.name.option() match {
        case None =>
          shape.withName("inline-type")
          shape.annotations += InlineElement()
        case Some("schema") | Some("type") =>
          shape.annotations += InlineElement()
        case _ if !shape.annotations.contains(classOf[DeclaredElement]) =>
          shape.annotations += InlineElement() // to catch media type named cases.
        case _ => // ignore
      }
    }
    val linkLabel = shape.name.option().getOrElse(ctx.nextTypeName)
    val linked = shape match {
      // if it is recursive we force the conversion into a linked shape
      case rec: RecursiveShape =>
        val hash = s"${rec.id}$linkLabel".hashCode
        RecursiveShape()
          .withId(s"${rec.id}/link-$hash")
          .withLinkTarget(rec)
          .withLinkLabel(linkLabel)
      // no recursive we just generate the linked shape
      case _ =>
        shape.link[Shape](linkLabel)
    }

    link(b, linked, inArray)
  }

  protected def link(b: Part[T], elementWithLink: DomainElement with Linkable, inArray: Boolean = false): Unit = {
    def emit(b: Part[T]): Unit = {
      // before emitting, we remove the link target to avoid loops and set
      // the fresh value for link-id
      val savedLinkTarget = elementWithLink.linkTarget
      elementWithLink.linkTarget.foreach { target =>
        elementWithLink.set(LinkableElementModel.TargetId, target.id)
        elementWithLink.fields.removeField(LinkableElementModel.Target)
      }
      b.obj { o =>
        emitAmfObject(elementWithLink, o)
      }
      // we reset the link target after emitting
      savedLinkTarget.foreach { target =>
        elementWithLink.fields.setWithoutId(LinkableElementModel.Target, target)
      }
    }

    if (inArray) emit(b) else b.list(emit)
  }

  protected def emitAmfObject(amfObj: AmfObject, entry: Entry[T]): Unit

  protected def isSemanticExtension(extension: DomainExtension): Boolean = Option(extension.extension).isEmpty

  protected def emitDomainExtensions(element: AmfObject, b: Entry[T]): Unit = {
    val customProperties: ListBuffer[String] = ListBuffer()

    // Collect element custom annotations
    element.fields
      .entry(DomainElementModel.CustomDomainProperties)
      .map(_.value.value)
      .foreach {
        case AmfArray(values, _) =>
          val extensions = values.asInstanceOf[Seq[DomainExtension]]

          val groupedExtensions = extensions.legacyGroupBy(_.definedBy.id)

          // we sort by extension ID and then access the unordered above hash map
          extensions
            .filter(!isSemanticExtension(_))
            .sortBy(_.id)
            .map(_.definedBy.id)
            .distinct
            .foreach { uri =>
              val extensions = groupedExtensions(uri)
              customProperties += uri
              emitGroupedDomainExtensions(b, uri, extensions, None)
            }
        case _ => // ignore
      }

    // Collect element scalar fields custom annotations
    var count = 1
    element.fields.foreach { case (f, v) =>
      v.value.annotations
        .collect({ case e: DomainExtensionAnnotation => e })
        .sortBy(_.extension.id)
        .foreach(e => {
          val extension = e.extension
          val uri       = s"${element.id}/scalar-valued/$count/${extension.name.value()}"
          customProperties += uri
          adoptTree(uri, extension.extension) // Fix ids
          emitGroupedDomainExtensions(b, uri, Seq(extension), Some(f))
          count += 1
        })
    }

    if (customProperties.nonEmpty)
      b.entry(
        ctx.emitIri(DomainElementModel.CustomDomainProperties.value.iri()),
        _.list { b =>
          customProperties.foreach(iri(b, _, inArray = true))
        }
      )
  }

  protected def emitGroupedDomainExtensions(
      b: Entry[T],
      uri: String,
      extensions: Seq[DomainExtension],
      field: Option[Field] = None
  ): Unit = {
    extensions.size match {
      case 1 =>
        b.entry(
          uri,
          _.obj { objectBuilder =>
            createCustomExtensionNode(objectBuilder, uri, extensions.head, field)
          }
        )
      case x if x > 1 =>
        b.entry(
          uri,
          _.list { listBuilder =>
            extensions.foreach { extension =>
              listBuilder.obj { objectBuilder => createCustomExtensionNode(objectBuilder, uri, extension, field) }
            }
          }
        )
      case _ => // ignore, maybe throw validation?
    }
  }

  protected def createCustomExtensionNode(
      b: Entry[T],
      uri: String,
      extension: DomainExtension,
      field: Option[Field] = None
  ): Unit

  protected def emitDate(v: Value, b: Part[T]): Unit = {
    val maybeDateTime = v.value.asInstanceOf[AmfScalar].value match {
      case dt: SimpleDateTime => Some(dt)
      case other              => SimpleDateTime.parse(other.toString).toOption
    }
    maybeDateTime match {
      case Some(dateTime) =>
        if (dateTime.timeOfDay.isDefined || dateTime.zoneOffset.isDefined) {
          typedScalar(b, dateTime.toString, DataType.DateTime)
        } else {
          typedScalarFrom(b, dateTime)
        }
      case _ =>
        emitScalar(b, v.value)
    }
  }

  protected def emitSimpleDateTime(b: Part[T], dateTime: SimpleDateTime, inArray: Boolean = true): Unit = {
    if (dateTime.timeOfDay.isDefined || dateTime.zoneOffset.isDefined) {
      typedScalar(b, dateTime.toString, DataType.DateTime, inArray)
    } else {
      typedScalarFrom(b, dateTime)
    }
  }

  private def typedScalarFrom(b: Part[T], dateTime: SimpleDateTime): Unit = {
    typedScalar(b, f"${dateTime.year}%04d-${dateTime.month}%02d-${dateTime.day}%02d", DataType.Date)
  }

  protected def emitArrayObjectMember(b: Part[T], member: AmfObject): Unit

  protected def value(t: Type, v: Value, parent: String, sources: Value => Unit, b: Part[T]): Unit = {
    t match {
      case _: ShapeModel if ctx.canGenerateLink(v.value) =>
        extractToLink(v.value.asInstanceOf[Shape], b)
      case t: DomainElement with Linkable if t.isLink =>
        link(b, t)
        sources(v)
      case _: Obj =>
        obj(b, v.value.asInstanceOf[AmfObject])
        sources(v)
      case Iri | EncodedIri =>
        iri(b, v.value.asInstanceOf[AmfScalar].toString)
        sources(v)
      case LiteralUri =>
        typedScalar(b, v.value.asInstanceOf[AmfScalar].toString, DataType.AnyUri)
        sources(v)
      case Str =>
        emitScalar(b, v.value)
        sources(v)
      case Bool =>
        emitScalar(b, v.value, SType.Bool)
        sources(v)
      case Type.Int | Type.Long =>
        emitScalar(b, v.value, SType.Int)
        sources(v)
      case Type.Double | Type.Float =>
        // Doubles get emitted as floats without the @type entry
        emitScalar(b, v.value, SType.Float)
        sources(v)
      case Type.DateTime =>
        // Changing this to handle both: standard cases of datetime and JsonLdSchema cases
        val dateTimeValue = v.value.asInstanceOf[AmfScalar].value.toString
        typedScalar(b, dateTimeValue, DataType.DateTime)
        sources(v)
      case Type.Date =>
        emitDate(v, b)
        sources(v)
      case Type.Null =>
        emitNull(b)
        sources(v)
      case a: SortedArray =>
        createSortedArray(a.element, v, b, parent, sources)
      case a: Array => emitArray(a, v, b, sources)
      case Any if v.value.isInstanceOf[AmfScalar] =>
        v.value.asInstanceOf[AmfScalar].value match {
          case bool: Boolean       => typedScalar(b, bool.toString, DataType.Boolean, inArray = true)
          case i: Int              => typedScalar(b, i.toString, DataType.Integer, inArray = true)
          case f: Float            => typedScalar(b, f.toString, DataType.Float, inArray = true)
          case d: Double           => typedScalar(b, d.toString, DataType.Double, inArray = true)
          case sdt: SimpleDateTime => emitSimpleDateTime(b, sdt)
          case other               => scalar(b, other.toString)
        }
    }
  }

  protected def emitNull(b: Part[T]) = {
    b += Scalar(SType.Null, null)
  }
  protected def emitArray(a: Array, v: Value, b: Part[T], sources: Value => Unit) = {
    b.list { b =>
      val seq = v.value.asInstanceOf[AmfArray]
      sources(v)
      createArrayValues(a, seq, b, v)
    }
  }

  protected def emitObjMember(amfObject: AmfObject, b: Part[T], inArray: Boolean) = amfObject match {
    case v @ (_: Shape) if ctx.canGenerateLink(v) =>
      extractToLink(v, b, inArray = true)
    case elementInArray: DomainElement with Linkable if elementInArray.isLink =>
      link(b, elementInArray, inArray = true)
    case elementInArray =>
      obj(b, elementInArray, inArray = inArray)
  }

  protected def createArrayValues(a: Array, seq: AmfArray, b: Part[T], v: Value): Unit = {
    a.element match {
      case _: Obj =>
        seq.values.asInstanceOf[Seq[AmfObject]].foreach(emitObjMember(_, b, inArray = true))
      case Str =>
        seq.values.asInstanceOf[Seq[AmfScalar]].foreach { e =>
          scalar(b, e.toString)
        }
      case EncodedIri | Iri =>
        seq.values.asInstanceOf[Seq[AmfScalar]].foreach(e => iri(b, e.toString, inArray = true))
      case LiteralUri =>
        typedScalar(b, v.value.asInstanceOf[AmfScalar].toString, DataType.AnyUri, inArray = true)
      case Type.Int | Type.Long =>
        seq.values
          .asInstanceOf[Seq[AmfScalar]]
          .foreach(e => scalar(b, e.value.toString, SType.Int))
      case Type.Float =>
        seq.values
          .asInstanceOf[Seq[AmfScalar]]
          .foreach(e => scalar(b, e.value.toString, SType.Float))
      case Bool =>
        seq.values
          .asInstanceOf[Seq[AmfScalar]]
          .foreach(e => scalar(b, e.value.toString, SType.Bool))
      case Type.DateTime =>
        seq.values
          .asInstanceOf[Seq[AmfScalar]]
          .foreach { e =>
            val dateTime = e.value.asInstanceOf[SimpleDateTime]
            typedScalar(b, dateTime.toString, DataType.DateTime)
          }
      case Type.Date =>
        seq.values
          .asInstanceOf[Seq[AmfScalar]]
          .foreach { e =>
            val dateTime = e.value.asInstanceOf[SimpleDateTime]
            emitSimpleDateTime(b, dateTime, inArray = false)
          }
      case Any =>
        seq.values.asInstanceOf[Seq[AmfScalar]].foreach(emitScalarMember(_, b))
      case _ => seq.values.asInstanceOf[Seq[AmfScalar]].foreach(e => iri(b, e.toString, inArray = true))
    }
  }

  protected def emitScalarMember(scalarElement: AmfScalar, b: Part[T]): Unit = {
    scalarElement.value match {
      case bool: Boolean =>
        typedScalar(b, bool.toString, DataType.Boolean, inArray = true)
      case i: Int              => typedScalar(b, i.toString, DataType.Integer, inArray = true)
      case f: Float            => typedScalar(b, f.toString, DataType.Float, inArray = true)
      case d: Double           => typedScalar(b, d.toString, DataType.Double, inArray = true)
      case sdt: SimpleDateTime => emitSimpleDateTime(b, sdt)
      case other               => scalar(b, other.toString)
    }
  }

  protected def createSortedArray(a: Type, v: Value, b: Part[T], parent: String, sources: Value => Unit): Unit

  protected def obj(b: Part[T], obj: AmfObject, inArray: Boolean = false): Unit

  protected def emitArrayMember(element: Type, e: AmfElement, b: Part[T]): Unit = {
    element match {
      case _: Obj =>
        e match {
          case elementInArray: DomainElement with Linkable if elementInArray.isLink =>
            link(b, elementInArray, inArray = true)
          case elementInArray: AmfObject =>
            emitArrayObjectMember(b, elementInArray)
        }
      case Str =>
        scalar(b, e, SType.Str)

      case EncodedIri =>
        iri(b, e.asInstanceOf[AmfScalar].toString, inArray = true)

      case Iri =>
        iri(b, e.asInstanceOf[AmfScalar].toString, inArray = true)

      case Any =>
        val scalarElement = e.asInstanceOf[AmfScalar]
        scalarElement.value match {
          case bool: Boolean =>
            typedScalar(b, bool.toString, DataType.Boolean, inArray = true)
          case str: String =>
            typedScalar(b, str, DataType.String, inArray = true)
          case i: Int =>
            typedScalar(b, i.toString, DataType.Integer, inArray = true)
          case d: Double =>
            typedScalar(b, d.toString, DataType.Double, inArray = true)
          case f: Float =>
            typedScalar(b, f.toString, DataType.Float, inArray = true)
          case other => scalar(b, other.toString)
        }
    }
  }

  protected def createSourcesNode(id: String, sources: SourceMap, b: Entry[T]): Unit = {
    val filteredSources = if (options.governanceMode) filterSourceMaps(sources) else sources
    val withSourceMaps  = options.isWithSourceMaps || options.governanceMode

    if (withSourceMaps && filteredSources.nonEmpty) {
      if (options.isWithRawSourceMaps) {
        b.entry(
          "smaps",
          _.obj { b =>
            createAnnotationNodes(id, b, filteredSources.annotations)
            createAnnotationNodes(id, b, filteredSources.eternals)
          }
        )
      } else {
        b.entry(
          ctx.emitIri(DomainElementModel.Sources.value.iri()),
          _.list {
            _.obj { b =>
              emitAnnotations(id, filteredSources, b)
            }
          }
        )
      }
    } else {
      createEternalsAnnotationsNodes(id, options, b, filteredSources)
    }
  }

  protected def emitAnnotations(id: String, filteredSources: SourceMap, b: Entry[T]): Unit

  protected def createEternalsAnnotationsNodes(
      id: String,
      options: RenderOptions,
      b: Entry[T],
      sources: SourceMap
  ): Unit = {
    if (sources.eternals.nonEmpty)
      if (options.isWithRawSourceMaps) {
        b.entry(
          "smaps",
          _.obj { b =>
            createAnnotationNodes(id, b, sources.eternals)
          }
        )
      } else {
        b.entry(
          ctx.emitIri(DomainElementModel.Sources.value.iri()),
          _.list {
            _.obj { b =>
              emitEternalsNode(id, sources, b)
            }
          }
        )
      }
  }

  protected def emitEternalsNode(id: String, sources: SourceMap, b: Entry[T]): Unit

}
