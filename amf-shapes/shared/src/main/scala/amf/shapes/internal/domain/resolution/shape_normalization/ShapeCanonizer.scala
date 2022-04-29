package amf.shapes.internal.domain.resolution.shape_normalization

import amf.core.client.scala.model.domain._
import amf.core.client.scala.model.domain.extensions.PropertyShape
import amf.core.internal.annotations._
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.ShapeModel
import amf.core.internal.metamodel.domain.extensions.PropertyShapeModel
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.validation.CoreValidations.TransformationValidation
import amf.shapes.client.scala.model.domain._
import amf.shapes.internal.domain.metamodel._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[resolution] object ShapeCanonizer {
  def apply(s: Shape, context: NormalizationContext): Shape = ShapeCanonizer()(context).normalize(s)
}

sealed case class ShapeCanonizer()(implicit val context: NormalizationContext) extends ShapeNormalizer {

  protected def cleanUnnecessarySyntax(shape: Shape): Shape = {
    shape.annotations.reject(a => !a.isInstanceOf[PerpetualAnnotation])
    shape
  }

  private var withoutCaching = false

  private def runWithoutCaching[T](fn: () => T): T = {
    withoutCaching = true
    val t: T = fn()
    withoutCaching = false
    t
  }

  private def normalizeWithoutCaching(s: Shape): Shape = runWithoutCaching(() => normalize(s))

  private def actionWithoutCaching(s: Shape): Shape = runWithoutCaching(() => normalizeAction(s))

  override protected def normalizeAction(shape: Shape): Shape = {
    cleanUnnecessarySyntax(shape)
    val canonical = shape match {
      case union: UnionShape         => canonicalUnion(union)
      case scalar: ScalarShape       => canonicalScalar(scalar)
      case array: ArrayShape         => canonicalArray(array)
      case matrix: MatrixShape       => canonicalMatrix(matrix)
      case tuple: TupleShape         => canonicalTuple(tuple)
      case property: PropertyShape   => canonicalProperty(property)
      case fileShape: FileShape      => canonicalShape(fileShape)
      case nil: NilShape             => canonicalShape(nil)
      case node: NodeShape           => canonicalNode(node)
      case recursive: RecursiveShape => recursive
      case any: AnyShape             => canonicalAny(any)
    }
    if (!withoutCaching) context.cache + canonical // i should never add a shape if is not resolved yet
    context.cache.updateFixPoints(canonical, withoutCaching)

    canonical
  }

  protected def canonicalLogicalConstraints(shape: Shape): Unit = {
    var oldLogicalConstraints = shape.fields.getValue(ShapeModel.And)
    if (Option(oldLogicalConstraints).isDefined) {
      val newLogicalConstraints = shape.and.map(normalize)
      shape.setArrayWithoutId(ShapeModel.And, newLogicalConstraints, oldLogicalConstraints.annotations)
    }

    oldLogicalConstraints = shape.fields.getValue(ShapeModel.Or)
    if (Option(oldLogicalConstraints).isDefined) {
      val newLogicalConstraints = shape.or.map(normalize)
      shape.setArrayWithoutId(ShapeModel.Or, newLogicalConstraints, oldLogicalConstraints.annotations)
    }

    oldLogicalConstraints = shape.fields.getValue(ShapeModel.Xone)
    if (Option(oldLogicalConstraints).isDefined) {
      val newLogicalConstraints = shape.xone.map(normalize)
      shape.setArrayWithoutId(ShapeModel.Xone, newLogicalConstraints, oldLogicalConstraints.annotations)
    }

    val notConstraint = shape.fields.getValue(ShapeModel.Not)
    if (Option(notConstraint).isDefined) {
      val newLogicalConstraint = normalize(shape.not)
      shape.setWithoutId(ShapeModel.Not, newLogicalConstraint, notConstraint.annotations)
    }
  }

  private def canonicalShape(any: Shape) = {
    canonicalLogicalConstraints(any)
    if (any.inherits.nonEmpty) {
      canonicalInheritance(any)
    } else {
      any
    }
  }

  private def canonicalAny(any: AnyShape) = {
    canonicalLogicalConstraints(any)
    if (any.inherits.nonEmpty) {
      canonicalInheritance(any)
    } else {
      AnyShapeAdjuster(any)
    }
  }

  protected def canonicalScalar(scalar: ScalarShape): Shape = {
    canonicalLogicalConstraints(scalar)
    if (Option(scalar.inherits).isDefined && scalar.inherits.nonEmpty) {
      canonicalInheritance(scalar)
    } else {
      scalar
    }
  }

  protected def canonicalInheritance(shape: Shape): Shape = {
    if (endpointSimpleInheritance(shape)) {
      val referencedShape = shape.inherits.head
      aggregateExamples(shape, referencedShape)
      if (shape.annotations.contains(classOf[AutoGeneratedName])) referencedShape.add(AutoGeneratedName())
      if (
        !referencedShape
          .isInstanceOf[RecursiveShape]
      ) // i need to mark the reference shape as resolved to extract to declaration in graph emitter if is a declared element
        referencedShape.annotations += ResolvedInheritance()
      normalize(referencedShape)
    } else {
      val superTypes = shape.inherits
      val oldInheritsIds: Seq[Shape] = if (context.keepEditingInfo) shape.inherits.collect {
        case rec: RecursiveShape => rec
        case shape: Shape        => shape.link(shape.name.value()).asInstanceOf[Shape]
      }
      else Nil
      shape.fields.removeField(
        ShapeModel.Inherits
      ) // i need to remove the resolved type without inherits, because later it will be added to cache once it will be fully resolved
      var accShape: Shape                             = normalizeWithoutCaching(shape)
      var superShapesWithDiscriminator: Seq[AnyShape] = Nil
      var inheritedIds: Seq[String]                   = Nil

      superTypes.foreach { superNode =>
        val canonicalSuperNode = normalize(superNode)

        // we save this information to connect the references once we have computed the minShape
        if (hasDiscriminator(canonicalSuperNode))
          superShapesWithDiscriminator = superShapesWithDiscriminator ++ Seq(canonicalSuperNode.asInstanceOf[NodeShape])

        canonicalSuperNode match {
          case chain: InheritanceChain => inheritedIds ++= (Seq(canonicalSuperNode.id) ++ chain.inheritedIds)
          case _                       => inheritedIds :+= canonicalSuperNode.id
        }
        val newMinShape = context.minShape(accShape, canonicalSuperNode)
        accShape = actionWithoutCaching(newMinShape)
      }
      if (context.keepEditingInfo) accShape.annotations += InheritedShapes(oldInheritsIds.map(_.id))
      if (!shape.id.equals(accShape.id)) {
        context.cache.registerMapping(shape.id, accShape.id)
        accShape.withId(shape.id) // i need to override id, if not i will override the father cached shape
      }

      // adjust inheritance chain if discriminator is defined
      accShape match {
        case any: AnyShape => superShapesWithDiscriminator.foreach(_.linkSubType(any))
        case _             => // ignore
      }

      // we set the full set of inherited IDs
      accShape match {
        case chain: InheritanceChain => chain.inheritedIds ++= inheritedIds
        case _                       => // ignore
      }

      shape match {
        case any: AnyShape if isSimpleInheritance(any, superTypes) => aggregateExamples(superTypes.head, accShape)
        case _                                                     => // Nothing to do
      }

      accShape
    }
  }

  private def copyExamples(from: AnyShape, to: AnyShape): Unit = {
    from.examples.foreach(fromEx => {
      to.examples.find { toEx =>
        val sameId = fromEx.id == toEx.id
        sameId || areExamplesEqual(fromEx, toEx)
      } match {
        case Some(duplicatedExample) =>
          copyTracking(fromEx, duplicatedExample)
        case None =>
          to.setArrayWithoutId(AnyShapeModel.Examples, to.examples ++ Seq(fromEx))
      }
    })
  }

  /** 2 examples are equal when these 2 conditions are true:
    *   - their raw content is the same
    *   - their names are equal or not relevant (autogenerated or null)
    */
  private def areExamplesEqual(ex1: Example, ex2: Example): Boolean = {
    val oneIsGenerated = ex1.annotations.contains(classOf[AutoGeneratedName])
    val twoIsGenerated = ex2.annotations.contains(classOf[AutoGeneratedName])
    val sameRaw        = ex1.raw.option().getOrElse("").trim == ex2.raw.option().getOrElse("").trim
    val sameOrIrrelevantNames = {
      if (oneIsGenerated && twoIsGenerated) true
      else if (!oneIsGenerated && !twoIsGenerated) ex1.name.value() == ex2.name.value()
      else if (ex1.name.isNull || ex2.name.isNull) true // one name is generated, the other is null
      else false
    }
    sameRaw && sameOrIrrelevantNames
  }

  private def copyTracking(duplicate: Example, receiver: Example): Unit = {
    duplicate.annotations.find(classOf[TrackedElement]).foreach { dupAnnotation =>
      receiver.annotations += receiver.annotations
        .find(classOf[TrackedElement])
        .fold(TrackedElement(dupAnnotation.parents)) { receiverAnnotation =>
          receiver.annotations.reject(_.isInstanceOf[TrackedElement])
          TrackedElement(receiverAnnotation.parents ++ dupAnnotation.parents)
        }
    }
  }

  protected def aggregateExamples(shape: Shape, toShape: Shape): Unit = {
    (shape, toShape) match {
      case (from: AnyShape, to: AnyShape) =>
        copyExamples(from, to)
        if (to.examples.size > 1) checkExamplesNames(to.examples)
      case _ => // Nothing to do
    }
  }

  /** Generate a name for examples that have no name, or it's name is duplicated */
  private def checkExamplesNames(examples: Seq[Example]): Unit = {
    val definedNames = mutable.Set[String]()
    var index        = 0
    examples.foreach { example =>
      val exampleName = example.name.value()
      if (example.name.isNull || definedNames.contains(exampleName)) {
        val name = s"example_$index"
        definedNames.add(name)
        example.withName(name)
        example.add(Annotations(AutoGeneratedName()))
        index += 1
      } else definedNames.add(exampleName)
    }
  }

  private def isSimpleInheritance(shape: Shape, superTypes: Seq[Shape] = Seq()): Boolean = {
    shape match {
      case ns: NodeShape =>
        superTypes.size == 1 && ns.annotations.contains(classOf[DeclaredElement]) && ns.properties.isEmpty
      case _: AnyShape if superTypes.size == 1 =>
        val superType = superTypes.head
        val ignoredFields =
          Seq(
            ShapeModel.Inherits,
            ShapeModel.Name,
            ShapeModel.DisplayName,
            ShapeModel.Description,
            AnyShapeModel.Examples,
            AnyShapeModel.Documentation,
            AnyShapeModel.Comment
          )
        fieldsPresentInSuperType(shape, superType, ignoredFields)
      case _ => false
    }
  }

  private def endpointSimpleInheritance(shape: Shape): Boolean = shape match {
    case anyShape: AnyShape if anyShape.annotations.contains(classOf[DeclaredElement]) => false
    case anyShape: AnyShape =>
      anyShape match {
        case any: AnyShape if any.inherits.size == 1 =>
          val superType     = any.inherits.head
          val ignoredFields = Seq(ShapeModel.Inherits, AnyShapeModel.Examples, AnyShapeModel.Name)
          fieldsPresentInSuperType(shape, superType, ignoredFields)
        case _ => false
      }
  }

  private def fieldsPresentInSuperType(shape: Shape, superType: Shape, ignoredFields: Seq[Field] = Seq()): Boolean = {
    val effectiveFields = shape.fields.fields().filterNot(f => ignoredFields.contains(f.field))
    // To be a simple inheritance, all the effective fields of the shape must be the same in the superType
    effectiveFields.foreach(e => {
      superType.fields.entry(e.field) match {
        case Some(s) if s.value.value.equals(e.value.value)                              => // Valid
        case _ if e.field == NodeShapeModel.Closed && !superType.isInstanceOf[NodeShape] => // Valid
        case _                                                                           => return false
      }
    })
    true
  }

  protected def hasDiscriminator(shape: Shape): Boolean = {
    shape match {
      case anyShape: NodeShape => anyShape.discriminator.option().isDefined
      case _                   => false
    }
  }

  protected def canonicalArray(array: ArrayShape): Shape = {
    canonicalLogicalConstraints(array)
    if (array.inherits.nonEmpty) {
      canonicalInheritance(array)
    } else {
      Option(array.items).fold(array.asInstanceOf[Shape])(i => {
        val newItems = normalize(i)
        array.annotations += ExplicitField()
        array.fields.removeField(ArrayShapeModel.Items)
        newItems match {
          case _: ArrayShape =>
            // Array items -> array must become a Matrix
            array.fields.setWithoutId(ArrayShapeModel.Items, newItems)
            array.toMatrixShape
          case _ =>
            // No union, we just set the new canonical items
            array.fields.setWithoutId(ArrayShapeModel.Items, newItems)
            array
        }
      })
    }
  }

  protected def canonicalMatrix(matrix: MatrixShape): Shape = {
    canonicalLogicalConstraints(matrix)
    if (matrix.inherits.nonEmpty) {
      canonicalInheritance(matrix)
    } else {
      Option(matrix.items) match {
        case Some(items) =>
          val newItems = normalize(items)
          matrix.fields.removeField(ArrayShapeModel.Items)
          newItems match {
            case unionItems: UnionShape =>
              val newUnionItems = unionItems.anyOf.map {
                case a: ArrayShape => matrix.cloneShape(Some(context.errorHandler)).withItems(a)
                case o             => matrix.cloneShape(Some(context.errorHandler)).toArrayShape.withItems(o)
              }
              unionItems.setArrayWithoutId(UnionShapeModel.AnyOf, newUnionItems)
              Option(matrix.fields.getValue(ShapeModel.Name)) match {
                case Some(name) => unionItems.withName(name.toString)
                case _          => unionItems
              }
            case a: ArrayShape => matrix.withItems(a)
            case _             => matrix.toArrayShape.withItems(newItems)
          }
        case _ => matrix
      }
    }
  }

  protected def canonicalTuple(tuple: TupleShape): Shape = {
    canonicalLogicalConstraints(tuple)
    if (tuple.inherits.nonEmpty) {
      canonicalInheritance(tuple)
    } else {
      var acc: Seq[Seq[Shape]] = Seq(Seq())

      val sources: Seq[Seq[Shape]] = tuple.items.map { shape =>
        normalize(shape) match {
          case other: Shape => Seq(other)
        }
      }

      sources.foreach { source =>
        source.foreach { shape =>
          acc = acc.map(_ ++ Seq(shape))
        }
      }

      if (acc.length == 1) {
        tuple.fields.setWithoutId(
          TupleShapeModel.TupleItems,
          AmfArray(acc.head),
          Option(tuple.fields.getValue(TupleShapeModel.TupleItems)).map(_.annotations).getOrElse(Annotations())
        )
        tuple
      } else {
        acc.map { items =>
          val newTuple = tuple.cloneShape(Some(context.errorHandler))
          newTuple.fields.setWithoutId(
            TupleShapeModel.Items,
            AmfArray(items),
            Option(tuple.fields.getValue(TupleShapeModel.Items)).map(_.annotations).getOrElse(Annotations())
          )
        }
        val union = UnionShape()
        union.id = tuple.id + "resolved"
        union.withName(tuple.name.value())
        union
      }
    }
  }

  protected def canonicalNode(node: NodeShape): Shape = {
    canonicalLogicalConstraints(node)
    node.add(ExplicitField())
    if (node.inherits.nonEmpty) {
      canonicalInheritance(node)
    } else {
      // We start processing the properties by cloning the base node shape
      def ensureInheritanceAnnotations(property: PropertyShape, canonicalProperty: PropertyShape) = {
        val annotationOption              = property.annotations.find(classOf[InheritanceProvenance])
        val annotationOptionFromCanonical = canonicalProperty.annotations.find(classOf[InheritanceProvenance])

        (annotationOption, annotationOptionFromCanonical) match {
          case (Some(annotation), None) => canonicalProperty.annotations += annotation
          case _                        => // Nothing
        }
      }
      val canonicalProperties: Seq[PropertyShape] = node.properties.map { propertyShape =>
        normalize(propertyShape) match {
          case canonicalProperty: PropertyShape =>
            ensureInheritanceAnnotations(propertyShape, canonicalProperty)
            canonicalProperty
          case other =>
            context.errorHandler.violation(
              TransformationValidation,
              other.id,
              None,
              s"Resolution error: Expecting property shape, found $other",
              other.position(),
              other.location()
            )
            propertyShape
        }
      }
      node.setArrayWithoutId(NodeShapeModel.Properties, canonicalProperties)

    }
  }

  protected def canonicalProperty(property: PropertyShape): Shape = {
    property.fields.setWithoutId(
      PropertyShapeModel.Range,
      normalize(property.range),
      property.fields.getValue(PropertyShapeModel.Range).annotations
    )
    property
  }

  protected def canonicalUnion(union: UnionShape): Shape = {
    if (union.inherits.nonEmpty) {
      canonicalInheritance(union)
    } else {
      val anyOfAcc: ListBuffer[Shape] = ListBuffer()
      union.anyOf.foreach { unionMember: Shape =>
        val normalizedUnionMember = normalizeWithoutCaching(unionMember)
        normalizedUnionMember match {
          case nestedUnion: UnionShape =>
            nestedUnion.anyOf.foreach(e => anyOfAcc += e)
          case other: Shape =>
            anyOfAcc += other
        }
      }
      val anyOfAnnotations = Option(union.fields.getValue(UnionShapeModel.AnyOf)) match {
        case Some(anyOf) => anyOf.annotations
        case _           => Annotations()
      }

      union.fields.setWithoutId(UnionShapeModel.AnyOf, AmfArray(anyOfAcc), anyOfAnnotations)

      union
    }
  }
}
