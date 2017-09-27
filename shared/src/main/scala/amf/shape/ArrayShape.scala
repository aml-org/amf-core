package amf.shape

import amf.domain.{Annotations, Fields}
import amf.metadata.shape.ArrayShapeModel._
import org.yaml.model.YPart

/**
  * Array shape
  */
abstract class DataArrangementShape() extends Shape {
  def minItems: Int        = fields(MinItems)
  def maxItems: Int        = fields(MaxItems)
  def uniqueItems: Boolean = fields(UniqueItems)

  def withMinItems(minItems: Int): this.type           = set(MinItems, minItems)
  def withMaxItems(maxItems: Int): this.type           = set(MaxItems, maxItems)
  def withUniqueItems(uniqueItems: Boolean): this.type = set(UniqueItems, uniqueItems)

  def withScalarItems(): ScalarShape = {
    val scalar = ScalarShape()
    this.set(Items, scalar)
    scalar
  }

  def withNodeItems(): NodeShape = {
    val node = NodeShape()
    this.set(Items, node)
    node
  }

  def withArrayItems(): ArrayShape = {
    val array = ArrayShape()
    this.set(Items, array)
    array
  }

  override def adopted(parent: String): this.type = withId(parent + "/array/" + name)
}

case class ArrayShape(fields: Fields, annotations: Annotations) extends DataArrangementShape {
  def items: Shape                       = fields(Items)
  def withItems(items: Shape): this.type = set(Items, items)

  def toMatrixShape: MatrixShape = MatrixShape(fields, annotations)

  override def linkCopy() = ArrayShape().withId(id)
}

object ArrayShape {

  def apply(): ArrayShape = apply(Annotations())

  def apply(ast: YPart): ArrayShape = apply(Annotations(ast))

  def apply(annotations: Annotations): ArrayShape = ArrayShape(Fields(), annotations)

}

case class MatrixShape(fields: Fields, annotations: Annotations) extends DataArrangementShape {
  def items: Shape                       = fields(Items)
  def withItems(items: Shape): this.type = set(Items, items)

  def toArrayShape               = ArrayShape(fields, annotations)
  def toMatrixShape: MatrixShape = this

  override def linkCopy() = MatrixShape().withId(id)
}

object MatrixShape {

  def apply(): MatrixShape = apply(Annotations())

  def apply(ast: YPart): MatrixShape = apply(Annotations(ast))

  def apply(annotations: Annotations): MatrixShape = MatrixShape(Fields(), annotations)

}

case class TupleShape(fields: Fields, annotations: Annotations) extends DataArrangementShape {
  def items: Seq[Shape]                       = fields(Items)
  def withItems(items: Seq[Shape]): this.type = setArray(Items, items)

  override def linkCopy() = TupleShape().withId(id)
}

object TupleShape {

  def apply(): TupleShape = apply(Annotations())

  def apply(ast: YPart): TupleShape = apply(Annotations(ast))

  def apply(annotations: Annotations): TupleShape = TupleShape(Fields(), annotations)

}
