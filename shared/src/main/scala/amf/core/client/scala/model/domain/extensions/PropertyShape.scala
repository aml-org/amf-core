package amf.core.client.scala.model.domain.extensions

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.metamodel.domain.ShapeModel
import amf.core.internal.metamodel.domain.extensions.PropertyShapeModel
import amf.core.internal.metamodel.domain.extensions.PropertyShapeModel._
import amf.core.client.scala.model.domain.{DomainElement, Linkable, Shape}
import amf.core.client.scala.model.{IntField, StrField}
import amf.core.internal.parser.domain.Fields
import amf.core.client.scala.traversal.ModelTraversalRegistry
import amf.core.internal.utils.AmfStrings
import amf.core.internal.parser.domain.{Annotations, Fields}

/**
  * Property shape
  */
case class PropertyShape(fields: Fields, annotations: Annotations) extends Shape {

  def path: StrField               = fields.field(Path)
  def range: Shape                 = fields.field(Range)
  def minCount: IntField           = fields.field(MinCount)
  def maxCount: IntField           = fields.field(MaxCount)
  def patternName: StrField        = fields.field(PatternName)
  def serializationOrder: IntField = fields.field(SerializationOrder)

  def withSerializationOrder(order: Int): this.type = set(SerializationOrder, order)

  def withPath(path: String): this.type  = set(Path, path)
  def withRange(range: Shape): this.type = set(Range, range)

  def withMinCount(min: Int): this.type           = set(MinCount, min)
  def withMaxCount(max: Int): this.type           = set(MaxCount, max)
  def withPatternName(pattern: String): this.type = set(PatternName, pattern)

  override def adopted(parent: String, cycle: Seq[String] = Seq()): this.type = {
    simpleAdoption(parent)
    if (Option(range).isDefined)
      range.adopted(id, cycle :+ id)
    this
  }

  override def linkCopy(): PropertyShape = PropertyShape().withId(id)

  override def meta: ShapeModel = PropertyShapeModel

  override def cloneShape(recursionErrorHandler: Option[AMFErrorHandler],
                          withRecursionBase: Option[String],
                          traversal: ModelTraversalRegistry,
                          cloneExamples: Boolean = false): PropertyShape = {
    val cloned = PropertyShape(Annotations(annotations))
    cloned.id = this.id
    copyFields(recursionErrorHandler, cloned, withRecursionBase, traversal)
    cloned.asInstanceOf[this.type]
  }

  /** Value , path + field value that is used to compose the id when the object its adopted */
  private[amf] override def componentId: String =
    "/property/" + name.option().getOrElse("default-property").urlComponentEncoded

  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement = PropertyShape.apply

  override def copyElement(): this.type = this
}

object PropertyShape {
  def apply(): PropertyShape = apply(Annotations())

  def apply(annotations: Annotations): PropertyShape = PropertyShape(Fields(), annotations)
}
