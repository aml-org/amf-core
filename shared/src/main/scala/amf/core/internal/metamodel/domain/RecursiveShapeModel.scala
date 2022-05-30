package amf.core.internal.metamodel.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Iri
import amf.core.client.scala.model.domain.{AmfObject, RecursiveShape}
import amf.core.client.scala.vocabulary.{Namespace, ValueType}

/** Recursion on a Shape structure, used when expanding a shape and finding the canonical representation of that shape.
  */
object RecursiveShapeModel extends ShapeModel {

  /** Link to the base of the recursion for a recursive shape
    */
  val FixPoint = Field(
      Iri,
      Namespace.Shapes + "fixPoint",
      ModelDoc(ModelVocabularies.Shapes, "fixpoint", "Link to the base of the recursion for a recursive shape")
  )

  override def fields: List[Field] = List(FixPoint) ++ ShapeModel.fields ++ DomainElementModel.fields

  override val `type`: List[ValueType] =
    List(Namespace.Shapes + "RecursiveShape") ++ ShapeModel.`type`

  override def modelInstance: AmfObject = RecursiveShape()

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Shapes,
      "RecursiveShape",
      "Recursion on a Shape structure, used when expanding a shape and finding the canonical representation of that shape."
  )
}
