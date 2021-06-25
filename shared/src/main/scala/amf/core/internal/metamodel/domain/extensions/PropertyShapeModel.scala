package amf.core.internal.metamodel.domain.extensions

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Bool, Int, Iri, Str}
import amf.core.internal.metamodel.domain._
import amf.core.client.scala.model.domain.extensions.PropertyShape
import amf.core.client.scala.vocabulary.Namespace.{Shacl, Shapes}
import amf.core.client.scala.vocabulary.ValueType

/**
  * Property shape metamodel
  *
  * Model for SHACL PropertyShapes
  */
object PropertyShapeModel extends ShapeModel {

  val Path =
    Field(Iri, Shacl + "path", ModelDoc(ExternalModelVocabularies.Shacl, "path", "Path to the constrained property"))

  val Range =
    Field(ShapeModel, Shapes + "range", ModelDoc(ModelVocabularies.Shapes, "range", "Range property constraint"))

  val SerializationOrder =
    Field(Int, Shapes + "serializationOrder", ModelDoc(ModelVocabularies.Shapes, "serializationOrder", "position in the set of properties for a shape used to serialize this property on the wire"))

  val MinCount = Field(Int,
                       Shacl + "minCount",
                       ModelDoc(ExternalModelVocabularies.Shacl, "minCount", "Minimum count property constraint"))

  val MaxCount = Field(Int,
                       Shacl + "maxCount",
                       ModelDoc(ExternalModelVocabularies.Shacl, "maxCount", "Maximum count property constraint"))

  val PatternName = Field(Str,
                          Shapes + "patternName",
                          ModelDoc(ModelVocabularies.Shapes, "patternName", "Patterned property constraint"))

  override val `type`: List[ValueType] = List(Shacl + "PropertyShape") ++ ShapeModel.`type`

  override def fields: List[Field] =
    List(Path, Range, MinCount, MaxCount, PatternName, SerializationOrder) ++ ShapeModel.fields ++ DomainElementModel.fields

  override def modelInstance = PropertyShape()

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Shapes,
      "PropertyShape",
      "Constraint over a property in a data shape."
  )
}
