package amf.core.internal.plugins.document.graph.emitter

import amf.core.client.scala.config
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.{BaseUnit, EncodesModel, SourceMap}
import amf.core.client.scala.model.domain._
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.vocabulary.{Namespace, NamespaceAliases}
import amf.core.internal.metamodel.Type._
import amf.core.internal.metamodel._
import amf.core.internal.metamodel.document.{BaseUnitModel, FragmentModel, ModuleModel, SourceMapModel}
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel
import amf.core.internal.parser.domain.{FieldEntry, Value}
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import amf.core.internal.plugins.document.graph.emitter.flattened.utils.{Emission, EmissionQueue, Metadata}
import org.yaml.builder.DocBuilder
import org.yaml.builder.DocBuilder.{Entry, Part, SType, Scalar}

import scala.language.implicitConversions

object FlattenedJsonLdEmitter {

  def emit[T](
      unit: BaseUnit,
      builder: DocBuilder[T],
      renderOptions: RenderOptions = config.RenderOptions(),
      namespaceAliases: NamespaceAliases = Namespace.defaultAliases,
      fieldProvision: ApplicableMetaFieldRenderProvider
  ): Boolean = {
    implicit val ctx: FlattenedGraphEmitterContext =
      FlattenedGraphEmitterContext(unit, renderOptions, namespaceAliases = namespaceAliases)
    new FlattenedJsonLdEmitter[T](builder, renderOptions, fieldProvision).root(unit)
    true
  }
}

class FlattenedJsonLdEmitter[T](
    val builder: DocBuilder[T],
    val options: RenderOptions,
    val fieldProvision: ApplicableMetaFieldRenderProvider
)(implicit ctx: FlattenedGraphEmitterContext)
    extends CommonEmitter[T, FlattenedGraphEmitterContext](options)
    with MetaModelTypeMapping {

  val pending: EmissionQueue[T] = EmissionQueue()
  var root: Part[T]             = _

  def root(unit: BaseUnit): Unit = {
    builder.obj { ob =>
      // Initialize root object
      ob.entry(
        JsonLdKeywords.Graph,
        _.list { rootBuilder =>
          root = rootBuilder

          /** First queue non declaration elements. We do this because these elements can generate new declarations that
            * we need to know before emitting the Base Unit.
            */
          val declarationsEntry: Option[FieldEntry] = unit.fields.entry(ModuleModel.Declares)
          val referencesEntry: Option[FieldEntry]   = unit.fields.entry(ModuleModel.References)

          extractDeclarationsAndReferencesToContext(declarationsEntry, referencesEntry, unit.annotations)

          unit.fields.removeField(ModuleModel.Declares)
          unit.fields.removeField(ModuleModel.References)

          unit match {
            case u: EncodesModel if isSelfEncoded(u) =>
              /** If it self encoded we do not queue the encodes node because it will be emitted in the same node as the
                * base unit
                */
              queueObjectFieldValues(u, (f, _) => f != FragmentModel.Encodes)
              queueObjectFieldValues(u.encodes) // Still need to queue encodes elements
            case _ =>
              queueObjectFieldValues(unit)
          }

          while (pending.hasPendingEmissions) {
            val emission = pending.nextEmission()
            emission.fn(root)
          }

          /** Emit Base Unit. This will emit declarations also. We don't render the already rendered elements because
            * the queue avoids duplicate ids
            */
          if (isSelfEncoded(unit)) {
            emitSelfEncodedBaseUnitNode(unit)
          } else {
            emitBaseUnitNode(unit)
          }

          // Check added declarations
          checkPendingEmissions()

          // Now process external links, not declared as part of the unit
          while (pending.hasPendingExternalEmissions) {
            val emission = pending.nextExternalEmission()
            ctx.emittingDeclarations = emission.isDeclaration
            ctx.emittingReferences = emission.isReference
            emission.fn(root)
          }
          // new regular nodes might have been generated, annotations for example
          checkPendingEmissions()
        }
      )
      ctx.emitContext(ob)
    }
  }

  private def checkPendingEmissions(): Unit = {
    while (pending.hasPendingEmissions) {
      val emission = pending.nextEmission()
      ctx.emittingDeclarations = emission.isDeclaration
      ctx.emittingReferences = emission.isReference
      emission.fn(root)
    }
  }

  private def isSelfEncoded(unit: BaseUnit): Boolean = {
    unit match {
      case e: EncodesModel => Option(e.encodes).exists(_.id == e.id)
      case _               => false
    }
  }

  def queueObjectFieldValues(amfObject: AmfObject, filter: (Field, Value) => Boolean = (_, _) => true): Unit = {
//  should filter fields with getMetaModelFields(amfObject, amfObject.meta, extensionIris, options)
    amfObject.fields.foreach {
      case (field, value) if filter(field, value) && filterSourceInformationNode(field, options) =>
        field.`type` match {
          case _: Obj =>
            val valueObj = value.value.asInstanceOf[AmfObject]
            pending.tryEnqueue(valueObj)
          case _: ArrayLike =>
            val valueArray = value.value.asInstanceOf[AmfArray]
            valueArray.values.foreach {
              case valueObj: AmfObject => pending.tryEnqueue(valueObj)
              case _                   => // Ignore
            }
          case _ => // Ignore
        }
      case _ => // Ignore
    }
  }

  def filterSourceInformationNode(f: Field, options: RenderOptions): Boolean = {
    !(f == BaseUnitModel.SourceInformation && !options.sourceInformation)
  }

  def emitSelfEncodedBaseUnitNode(unit: BaseUnit): Unit = {
    unit match {
      case u: EncodesModel =>
        root.obj { b =>
          val id         = u.id
          val unitObj    = metaModel(u)
          val encodedObj = metaModel(u.encodes)

          createIdNode(b, id)

          val allTypes = getTypesAsIris(unitObj) ++ getTypesAsIris(encodedObj)
          createTypeNode(b, allTypes)

          emitReferences(b, id, SourceMap(id, unit))
          emitDeclarations(b, id, SourceMap(id, unit))

          val sources = SourceMap(id, unit)

          // Emit both unit and unit.encodes fields to the same node
          emitFields(id, u.encodes, sources, b, fieldProvision.fieldsFor(u.encodes, options))

          pending.skip(id) // Skip emitting encodes node (since it is the same as this node)
          emitFields(id, u, sources, b, fieldProvision.fieldsFor(u, options))

          emitDomainExtensions(u, b)

          val sourceMapId: String = sourceMapIdFor(id)
          createSourcesNode(sourceMapId, sources, b)

        }
      case _ => // Exception?
    }
  }

  def emitBaseUnitNode(unit: BaseUnit): Unit = {
    val id = unit.id

    root.obj { b =>
      createIdNode(b, id)
      emitReferences(b, unit.id, SourceMap(unit.id, unit))
      emitDeclarations(b, unit.id, SourceMap(unit.id, unit))

      val sources = SourceMap(id, unit)
      val obj     = metaModel(unit)
      createTypeNode(b, obj)
      traverseMetaModel(id, unit, sources, obj, b)
      emitDomainExtensions(unit, b)

      val sourceMapId: String = sourceMapIdFor(id)
      createSourcesNode(sourceMapId, sources, b)

    }

  }

  implicit def object2Emission(amfObject: AmfObject): Emission[T] with Metadata = {
    val id = amfObject.id

    val e = new Emission[T](_ =>
      root.obj { b =>
        emitObject(amfObject, b)
      }
    ) with Metadata
    e.id = Some(id)
    e.isDeclaration = ctx.emittingDeclarations
    e.isReference = ctx.emittingReferences
    e.isExternal = amfObject
      .isInstanceOf[DomainElement] && amfObject.asInstanceOf[DomainElement].isExternalLink.option().getOrElse(false)
    e
  }

  def emitObject(amfObject: AmfObject, b: Entry[T]): Unit = {
    val id = amfObject.id
    createIdNode(b, id)

    val sources = SourceMap(id, amfObject)

    val obj = metaModel(amfObject)
    createTypeNode(b, obj)
    traverseMetaModel(id, amfObject, sources, obj, b)

    emitDomainExtensions(amfObject, b)

    val sourceMapId: String = sourceMapIdFor(id)
    createSourcesNode(sourceMapId, sources, b)
  }

  def traverseMetaModel(id: String, element: AmfObject, sources: SourceMap, obj: Obj, b: Entry[T]): Unit = {
    val modelFields: Seq[Field] = fieldProvision.fieldsFor(element, options)
    emitFields(id, element, sources, b, modelFields)
  }

  private def emitFields(
      id: String,
      element: AmfObject,
      sources: SourceMap,
      b: Entry[T],
      modelFields: Seq[Field]
  ): Unit = {
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
    createIdNode(b, extension.extension.id)
    val e = new Emission((part: Part[T]) => {
      part.obj { rb =>
        rb.entry(
          ctx.emitIri(DomainExtensionModel.Name.value.iri()),
          emitScalar(_, extension.name.value())
        )
        field.foreach(f =>
          rb.entry(
            ctx.emitIri(DomainExtensionModel.Element.value.iri()),
            emitScalar(_, f.value.iri())
          )
        )
        emitObject(extension.extension, rb)
      }
    }) with Metadata
    e.id = Some(extension.extension.id)
    e.isDeclaration = ctx.emittingDeclarations
    e.isReference = ctx.emittingReferences
    pending.tryEnqueue(e)
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
    b.obj { b =>
      val id = s"$parent/list"
      createIdNode(b, id)
      val e = new Emission((part: Part[T]) => {
        part.obj { rb =>
          createIdNode(rb, id)
          rb.entry(JsonLdKeywords.Type, ctx.emitIri((Namespace.Rdfs + "Seq").iri()))
          seq.zipWithIndex.foreach { case (e, i) =>
            rb.entry(
              ctx.emitIri((Namespace.Rdfs + s"_${i + 1}").iri()),
              { b =>
                emitArrayMember(a, e, b)
              }
            )
          }
        }
      }) with Metadata
      e.id = Some(id)
      e.isDeclaration = ctx.emittingDeclarations
      e.isReference = ctx.emittingReferences
      pending.tryEnqueue(e)
    }
  }

  override protected def emitArrayObjectMember(b: Part[T], member: AmfObject): Unit = obj(b, member)

  override protected def obj(b: Part[T], obj: AmfObject, inArray: Boolean = false): Unit = {
    def emit(b: Part[T]): Unit = {
      b.obj(createIdNode(_, obj.id))
      pending.tryEnqueue(obj)
    }
    emit(b)
  }

  override protected def emitAmfObject(amfObj: AmfObject, entry: Entry[T]): Unit = {
    createIdNode(entry, amfObj.id)
    pending.tryEnqueue(amfObj)
  }

  override protected def emitAnnotations(id: String, filteredSources: SourceMap, b: Entry[T]): Unit = {
    createIdNode(b, id)
    val e = new Emission((part: Part[T]) => {
      part.obj { rb =>
        createIdNode(rb, id)
        createTypeNode(rb, SourceMapModel)
        createAnnotationNodes(id, rb, filteredSources.annotations)
        createAnnotationNodes(id, rb, filteredSources.eternals)
      }
    }) with Metadata
    e.id = Some(id)
    e.isDeclaration = ctx.emittingDeclarations
    e.isReference = ctx.emittingReferences
    pending.tryEnqueue(e)
  }

  override protected def emitEternalsNode(id: String, sources: SourceMap, b: Entry[T]): Unit = {
    createIdNode(b, id)
    val e = new Emission((part: Part[T]) => {
      part.obj { rb =>
        createIdNode(rb, id)
        createTypeNode(rb, SourceMapModel)
        createAnnotationNodes(id, rb, sources.eternals)
      }
    }) with Metadata

    e.id = Some(id)
    e.isDeclaration = ctx.emittingDeclarations
    e.isReference = ctx.emittingReferences
    pending.tryEnqueue(e)
  }

  override protected def createAnnotationValueNode(id: String, b: Part[T], annotationEntry: (String, String)): Unit =
    annotationEntry match {
      case (iri, v) =>
        b.obj { b =>
          createIdNode(b, id)
        }
        val e = new Emission((part: Part[T]) => {
          part.obj { b =>
            createIdNode(b, id)
            b.entry(ctx.emitIri(SourceMapModel.Element.value.iri()), emitScalar(_, iri))
            b.entry(ctx.emitIri(SourceMapModel.Value.value.iri()), emitScalar(_, v))
          }
        }) with Metadata
        e.id = Some(id)
        e.isDeclaration = ctx.emittingDeclarations
        e.isReference = ctx.emittingReferences
        pending.tryEnqueue(e)
    }

  override protected def scalar(b: Part[T], content: String, t: SType): Unit = b += Scalar(t, content)

  override protected def emitScalar(b: Part[T], content: String, t: SType): Unit = scalar(b, content, t)

  override protected def emitScalar(b: Part[T], content: AmfElement, t: SType): Unit = scalar(b, content, t)

}
