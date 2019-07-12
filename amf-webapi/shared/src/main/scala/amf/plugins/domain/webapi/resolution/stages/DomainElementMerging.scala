package amf.plugins.domain.webapi.resolution.stages

import amf.core.annotations.DefaultNode
import amf.core.metamodel.domain.DomainElementModel._
import amf.core.metamodel.domain.templates.{KeyField, OptionalField}
import amf.core.metamodel.domain.{DataNodeModel, DomainElementModel, LinkableElementModel}
import amf.core.metamodel.{Field, Type}
import amf.core.model.DataType
import amf.core.model.domain.DataNodeOps.adoptTree
import amf.core.model.domain._
import amf.core.model.domain.extensions.PropertyShape
import amf.core.parser.{ErrorHandler, FieldEntry, Value}
import amf.core.utils.TemplateUri
import amf.plugins.document.webapi.annotations.{EmptyPayload, Inferred}
import amf.plugins.document.webapi.contexts.RamlWebApiContext
import amf.plugins.domain.shapes.metamodel.ScalarShapeModel
import amf.plugins.domain.shapes.models.ExampleTracking.tracking
import amf.plugins.domain.shapes.models.{AnyShape, ScalarShape}
import amf.plugins.domain.webapi.metamodel.{EndPointModel, OperationModel}
import amf.plugins.domain.webapi.models._
import amf.plugins.features.validation.ParserSideValidations.UnusedBaseUriParameter
import amf.plugins.features.validation.ResolutionSideValidations.{
  ResolutionValidation,
  UnequalMediaTypeDefinitionsInExtendsPayloads
}

import scala.collection.mutable

/**
  * Merge 'other' element into 'main' element:
  * 1) 'main' node properties are inspected and those that are undefined in 'other' node remain unchanged.
  * 2) 'main' node receives all properties of 'other' node (excluding optional ones), which are undefined in the 'main' node.
  * 3) Properties defined in both 'main' node and 'other' node (including optional ones) are treated as follows:
  *     a) Scalar properties remain unchanged.
  *     b) Collection properties are merged by value.
  *     c) Values of object properties are subjected to steps 1-3 of this procedure.
  */
case class DomainElementMerging()(implicit ctx: RamlWebApiContext) {

  def merge[T <: DomainElement](main: T, other: T, errorHandler: ErrorHandler): T = {
    MergingValidator.validate(main, other, errorHandler)
    var merged = false

    other.fields.fields().filter(ignored).foreach {
      case otherFieldEntry @ FieldEntry(otherField, _) =>
        main.fields.entry(otherField) match {
          case None =>
            // Case 2
            handleNewFieldEntry(main, otherFieldEntry)
          case Some(mainFieldEntry) =>
            // Cases 2 & 3 (check for some special conditions of case 2 where main field entry actually exists)
            merged = handleExistingFieldEntries(main, mainFieldEntry, otherFieldEntry, errorHandler)
        }
    }

    main match {
      case shape: Shape if merged => ensureNotRecursive(shape).asInstanceOf[T]
      case _                      => main
    }
  }

  def handleNewFieldEntry[T <: DomainElement](main: T, otherFieldEntry: FieldEntry): Unit = {
    val otherField = otherFieldEntry.field
    val otherValue = otherFieldEntry.value

    if (otherField == EndPointModel.Operations) {
      otherValue.value.asInstanceOf[AmfArray].values.foreach {
        case operation: Operation if !isOptional(OperationModel, operation) => ctx.mergeOperationContext(operation.id)
        case _                                                              => // Nothing
      }
    }

    otherField.`type` match {
      case t: OptionalField if isOptional(t, otherValue.value.asInstanceOf[DomainElement]) =>
      case Type.ArrayLike(otherElement) =>
        adoptNonOptionalArrayElements(main, otherField, otherValue, otherElement)
      case _ =>
        main.set(otherField, adoptInner(main.id, otherValue.value))
    }
  }

  def handleExistingFieldEntries[T <: DomainElement](main: T,
                                                     mainFieldEntry: FieldEntry,
                                                     otherFieldEntry: FieldEntry,
                                                     errorHandler: ErrorHandler): Boolean = {

    val otherField = otherFieldEntry.field
    val otherValue = otherFieldEntry.value
    val mainValue  = mainFieldEntry.value

    val mainFieldEntryHasValue = Option(mainValue).isDefined
    val mainFieldEntryValueIsDefined = Option(mainValue) match {
      case Some(_) => Option(mainValue.value).isDefined
      case None    => false
    }

    var shouldMerge = true

    // Try to match any of the special cases of case (2) where main entry is actually defined
    if (mainFieldEntryHasValue && mainFieldEntryValueIsDefined) {
      val mainValueIsAnyShape  = mainValue.value.isInstanceOf[AnyShape]
      val otherValueIsAnyShape = otherValue.value.isInstanceOf[AnyShape]
      val mainValueIsInferred  = mainValue.value.annotations.contains(classOf[Inferred])
      val mainValueIsDefault   = mainValue.value.annotations.contains(classOf[DefaultNode])

      mainFieldEntry match {
        case _ if mainValueIsAnyShape && otherValueIsAnyShape && mainValueIsInferred =>
          /**
            * Overwrite default-generated Any shapes by shapes coming from overlays/extensions
            * e.g. default value of a payload
            */
          val target = mainFieldEntry.value.value.asInstanceOf[AnyShape]
          val cloned = otherValue.value.asInstanceOf[AnyShape].cloneShape(None).withName(target.name.value())

          if (target.examples.nonEmpty) cloned.withExamples(target.examples)
          main.set(otherField, adoptInner(main.id, cloned))
          shouldMerge = false

        case _ if mainValueIsDefault =>
          /**
            * Existing element (mainValue) has an inferred default type. In the AST level merge, when the "type"
            * node is on the trait side it should be merged
            */
          otherField.`type` match {
            case t: OptionalField if isOptional(t, otherValue.value.asInstanceOf[DomainElement]) =>
            // Do nothing (Case 2)
            case Type.ArrayLike(otherElement) =>
              adoptNonOptionalArrayElements(main, otherField, otherValue, otherElement)
            case _: DomainElementModel =>
              mainValue.value match {
                // This case is for default type String (in parameters)
                case s: ScalarShape if s.dataType.value() == DataType.String =>
                  otherValue.value match {
                    // if both parts are scalar strings, then just merge the dataNodes
                    case sc: ScalarShape if sc.dataType.value() == DataType.String =>
                      merge(mainFieldEntry.domainElement, otherFieldEntry.domainElement, errorHandler)
                    // if other is an scalar with a different datatype
                    case sc: ScalarShape =>
                      s.set(ScalarShapeModel.DataType, sc.dataType.value())
                      merge(mainFieldEntry.domainElement, otherFieldEntry.domainElement, errorHandler)
                    // if other is an array or an object
                    case a: AnyShape =>
                      val examples = s.examples
                      main.set(otherField, adoptInner(main.id, a))
                      if (examples.nonEmpty)
                        main.fields
                          .entry(otherField)
                          .foreach(_.value.value.asInstanceOf[AnyShape].withExamples(examples))
                    // else override the shape
                    case x => main.set(otherField, adoptInner(main.id, x))
                  }
                // This case is for default type AnyShape (in payload in an endpoint)
                case _: AnyShape => merge(mainFieldEntry.domainElement, otherFieldEntry.domainElement, errorHandler)
                case _           => main.set(otherField, adoptInner(main.id, otherValue.value))
              }
            case _ => main.set(otherField, adoptInner(main.id, otherValue.value))
          }
          shouldMerge = false

        case _ => // Defaults to fallback (shouldMerge = true)
      }
    }

    // Case 3
    if (shouldMerge) {
      otherField.`type` match {
        case Type.Scalar(_) =>
        // Do nothing (3.a)
        case Type.ArrayLike(element) =>
          mergeByValue(main, otherField, element, mainValue, otherValue, errorHandler)
        case _: DomainElementModel =>
          merge(mainFieldEntry.domainElement, otherFieldEntry.domainElement, errorHandler)
        case _ =>
          errorHandler.violation(ResolutionValidation,
                                 main.id,
                                 s"Cannot merge '${otherField.`type`}':not a (Scalar|Array|Object)",
                                 main.annotations)
      }
    }
    shouldMerge
  }

  protected def ensureNotRecursive(shape: Shape, ids: Set[String] = Set()): Shape = {
    try {
      if (ids.contains(shape.id))
        shape match {
          case _: RecursiveShape => shape
          case p: PropertyShape  => p.withRange(RecursiveShape(p.range))
          case _                 => RecursiveShape(shape)
        } else {
        val newIds = ids ++ Seq(shape.id)
        shape.fields.foreach {
          case (f: Field, value: Value) =>
            val fieldValue  = value.value
            val annotations = value.annotations
            fieldValue match {
              case e: Shape =>
                shape.fields.setWithoutId(f, ensureNotRecursive(e, newIds), annotations)
              case arr: AmfArray =>
                val checked = arr.values.map {
                  case e: Shape => ensureNotRecursive(e, newIds)
                  case o        => o
                }
                shape.fields.setWithoutId(f, AmfArray(checked, arr.annotations))
              case o =>
                shape.fields.setWithoutId(f, o, annotations)
            }
        }
        shape
      }
    } catch {
      case _: Error => shape
    }
  }

  protected case class Adopted() {
    private var adopted: Seq[String] = Nil

    def +=(id: String): Adopted = {
      adopted = adopted :+ id
      this
    }

    def notYet(id: String): Boolean = !adopted.contains(id)
  }

  /**
    * Adopts recursively different kinds of AMF elements if not yet adopted
    *
    * @param parentId id of the adopter element
    * @param target element to be adopted
    * @param adopted utility class containing already adopted elements
    * @return adopted element with newly set ID
    */
  def adoptInner(parentId: String, target: AmfElement, adopted: Adopted = Adopted()): AmfElement = {
    target match {
      case array: AmfArray =>
        AmfArray(array.values.map(adoptInner(parentId, _, adopted)), array.annotations)
      case element: DomainElement if adopted notYet element.id =>
        val previousId = element.id
        adoptElementByType(element, parentId)
        adopted += element.id
        element.fields.foreach {
          case (f, value) =>
            if (ignored(FieldEntry(f, value))) {
              adoptInner(element.id, value.value, adopted)
            }
        }

        element match {
          case p: Payload => tracking(p.schema, element.id, Some(previousId))
          case _          =>
        }

        element
      case _ => target
    }
  }

  /**
    * Adopts target domain element by parent. (Makes element's ID relative to that of parent)
    *
    * @param target adopted
    * @param parentId id of the adopter element
    * @return adopted element with newly set ID
    */
  private def adoptElementByType(target: DomainElement, parentId: String) = {
    target match {
      case simple: Shape     => simple.simpleAdoption(parentId) // only shapes have recursive simple adoption?
      case dynamic: DataNode => DataNodeOps.adoptTree(parentId, dynamic)
      case _                 => target.adopted(parentId)
    }
  }

  private def adoptNonOptionalArrayElements(target: DomainElement,
                                            arrayField: Field,
                                            otherArrayValue: Value,
                                            otherArrayElementsType: Type): Unit = {
    otherArrayElementsType match {
      case t: OptionalField =>
        val nonOptionalElements: Seq[AmfElement] =
          otherArrayValue.value
            .asInstanceOf[AmfArray]
            .values
            .filter(v => !isOptional(t, v.asInstanceOf[DomainElement]))
        target.set(arrayField, adoptInner(target.id, AmfArray(nonOptionalElements)))
      case _ => target.set(arrayField, adoptInner(target.id, otherArrayValue.value))
    }
  }

  private def mergeByValue(target: DomainElement,
                           field: Field,
                           element: Type,
                           main: Value,
                           other: Value,
                           errorHandler: ErrorHandler): Unit = {
    val m = main.value.asInstanceOf[AmfArray]
    val o = other.value.asInstanceOf[AmfArray]

    element match {
      case _: Type.Scalar => mergeByValue(target, field, m, o)
      case key: KeyField  => mergeByKeyValue(target, field, element, key, m, o, errorHandler)
      case DataNodeModel  => mergeDataNodes(target, field, m, o)
      case _ =>
        errorHandler.violation(ResolutionValidation,
                               target.id,
                               s"Cannot merge '$element': not a KeyField nor a Scalar",
                               target.annotations)

    }
  }

  private def mergeDataNodes(target: DomainElement, field: Field, main: AmfArray, other: AmfArray): Unit = {

    val mainNodes  = main.values.asInstanceOf[Seq[DataNode]]
    val otherNodes = other.values.asInstanceOf[Seq[DataNode]]

    otherNodes.foreach {
      case oScalar: ScalarNode =>
        if (mainNodes
              .collectFirst({
                case ms: ScalarNode if ms.value.option().contains(oScalar.value.option().getOrElse("")) => ms
              })
              .isEmpty)
          target.add(field, oScalar)
      case other: DataNode => target.add(field, other)
    }
  }

  private def mergeByValue(target: DomainElement, field: Field, main: AmfArray, other: AmfArray): Unit = {
    val existing = main.values.map(_.asInstanceOf[AmfScalar].value).toSet
    other.values.foreach { value =>
      val scalar = value.asInstanceOf[AmfScalar].value
      if (!existing.contains(scalar)) {
        target.add(field, AmfScalar(scalar)) // Remove annotations so it is added last in the list.
      }
    }
  }

  private def mergeByKeyValue(target: DomainElement,
                              field: Field,
                              element: Type,
                              key: KeyField,
                              main: AmfArray,
                              other: AmfArray,
                              errorHandler: ErrorHandler): Unit = {

    val existing = main.values.map { m =>
      val obj = m.asInstanceOf[DomainElement]
      obj.fields.entry(key.key).map(_.scalar.value).getOrElse(None) -> obj
    }.toMap

    other.values.foreach { o =>
      val otherObj = o.asInstanceOf[DomainElement]
      otherObj.fields.entry(key.key) match {
        case Some(value) if !existing.contains(None) =>
          if (existing.contains(value.scalar.value)) {
            if (field == EndPointModel.Operations) {
              ctx.mergeOperationContext(otherObj.id)
            }
            merge(existing(value.scalar.value), otherObj.adopted(target.id), errorHandler)
          } else if (!isOptional(element, otherObj)) { // Case (2) -> If node is undefined in 'main' but is optional in 'other'.
            if (field == EndPointModel.Operations) {
              ctx.mergeOperationContext(otherObj.id)
            }
            target.add(field, adoptInner(target.id, o))
          }
        case None if !existing.forall(_._1 != None) =>
          if (existing.contains(None)) {
            merge(existing(None), otherObj.adopted(target.id), errorHandler)
          } else if (!isOptional(element, otherObj)) { // Case (2) -> If node is undefined in 'main' but is optional in 'other'.
            target.add(field, adoptInner(target.id, o))
          }
        case _ => //
      }
    }
  }

  private def isOptional(`type`: Type, obj: DomainElement) =
    `type`.isInstanceOf[OptionalField] && obj.fields
      .entry(`type`.asInstanceOf[OptionalField].Optional)
      .exists(_.scalar.toBool)

  private def ignored(entry: FieldEntry) = entry.field match {
    case Extends | Sources | LinkableElementModel.Target => false
    case _                                               => true
  }
}

/** Merge two data nodes of the same type. This merging applies the 'other' side as an overlay to the 'main' side. */
object DataNodeMerging {

  def merge(existing: DataNode, overlay: DataNode): Unit = {
    (existing, overlay) match {
      case (left: ScalarNode, right: ScalarNode) =>
        left.withValue(right.value.value(), right.value.annotations())
        left.withDataType(right.dataType.value(), right.dataType.annotations())
      case (left: ObjectNode, right: ObjectNode) =>
        mergeObjectNode(left, right)
      case (left: ArrayNode, right: ArrayNode) =>
        // Add members that are not in the left array.
        mergeArrayNode(left, right)
      case _ =>
    }
  }

  def mergeObjectNode(left: ObjectNode, right: ObjectNode): Unit =
    for { (key, value) <- right.propertyFields().map(f => (f, right.fields[DataNode](f))) } {
      left.fields.getValueAsOption(key) match {
        case Some(Value(property: DataNode, _)) => merge(property, value)
        case None                               => left.addPropertyByField(key, adoptTree(left.id, value), right.fields.getValue(key).annotations)
      }
    }

  /** Merge array data nodes by value: If scalar, check it's not there and add. If object or array, just add but adoptInner ids. */
  private def mergeArrayNode(main: ArrayNode, other: ArrayNode): Unit = {
    val existing = main.members.collect { case s: ScalarNode => s.value }

    other.members.foreach {
      case scalar: ScalarNode =>
        if (!existing.contains(scalar.value)) main.addMember(scalar)
      case node =>
        main.addMember(adoptTree(main.id, node))
    }
  }
}

/**
  * Checks some conditions when merging some nodes
  */
object MergingValidator {
  val reportedMissingParameters: mutable.Map[String, Seq[Parameter]] = mutable.Map.empty

  def validate[T <: DomainElement](main: T, other: T, errorHandler: ErrorHandler): Unit = {
    (main, other) match {
      case (m: Request, o: Request) =>
        validatePayloads(main, errorHandler, m.payloads, o.payloads)

      case (m: Response, o: Response) =>
        validatePayloads(main, errorHandler, m.payloads, o.payloads)

      case (m: EndPoint, o: EndPoint) =>
        validateEndpoints(main, errorHandler, m, o)

      case _ => // Nothing
    }
  }

  private def validateEndpoints[T <: DomainElement](main: T,
                                                    errorHandler: ErrorHandler,
                                                    m: EndPoint,
                                                    o: EndPoint): Unit = {

    val pathParams = TemplateUri.variables(m.path.value())
    val checkParameter = (p: Parameter, isOperation: Boolean) => {
      val maybeReported = reportedMissingParameters.get(m.id)

      val reported = maybeReported match {
        case Some(alreadyReported) => alreadyReported.exists(_.name.value() == p.name.value())
        case _                     => false
      }

      val unused = !p.name.option().exists(n => pathParams.contains(n))

      if (!reported && unused) {
        reportedMissingParameters += m.id -> (reportedMissingParameters.getOrElse(m.id, Seq.empty) :+ p)
        val message = if (isOperation) {
          s"Unused operation uri parameter ${p.name.value()}"
        } else {
          s"Unused uri parameter ${p.name.value()}"
        }
        errorHandler.warning(UnusedBaseUriParameter, p.id, None, message, p.position(), p.location())
      }
    }

    o.parameters
      .foreach(checkParameter(_, false))

    o.operations
      .flatMap(o => Option(o.request))
      .flatMap(_.uriParameters)
      .foreach(checkParameter(_, true))
  }

  private def validatePayloads[T <: DomainElement](main: T,
                                                   errorHandler: ErrorHandler,
                                                   m: Seq[Payload],
                                                   o: Seq[Payload]): Unit = {

    val shouldRevise = (payloads: Seq[Payload]) => {
      payloads.nonEmpty && payloads.forall { payload =>
        payload.isInstanceOf[Payload] && !payload.annotations.contains(classOf[EmptyPayload])
      }
    }

    val shouldReviseBoth = shouldRevise(m) && shouldRevise(o)

    if (shouldReviseBoth) {
      val mainPayloadsDefineMediaType = m.forall { payload =>
        payload.mediaType.option().isDefined
      }

      val otherPayloadsDefineMediaType = o.forall { payload =>
        payload.mediaType.option().isDefined
      }

      if (mainPayloadsDefineMediaType != otherPayloadsDefineMediaType) {

        errorHandler.violation(UnequalMediaTypeDefinitionsInExtendsPayloads,
                               main.id,
                               None,
                               UnequalMediaTypeDefinitionsInExtendsPayloads.message,
                               main.position(),
                               main.location())
      }
    }
  }
}