package amf.core.internal.metamodel.domain.extensions

import amf.core.client.scala.model.domain.extensions.CustomDomainProperty
import amf.core.client.scala.vocabulary.Namespace._
import amf.core.client.scala.vocabulary.ValueType
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Int, Iri, Str, Bool}
import amf.core.internal.metamodel.domain._
import amf.core.internal.metamodel.domain.common.{DescribedElementModel, DisplayNameField}
import amf.core.internal.metamodel.domain.templates.KeyField

/** Custom Domain Property
  *
  * Definition of an extension to the domain model defined directly by a user in the RAML/OpenAPI document.
  *
  * This can be achieved by using an annotationType in RAML. In OpenAPI thy don't need to be declared, they can just be
  * used.
  *
  * This should be mapped to new RDF properties declared directly in the main document or module.
  *
  * Contrast this extension mechanism with the creation of a propertyTerm in a vocabulary, a more re-usable and generic
  * way of achieving the same functionality
  */
object CustomDomainPropertyModel extends DomainElementModel with KeyField with DisplayNameField with DescribedElementModel {

  /** The name of the extension
    */
  val Name: Field = Field(Str, Core + "name", ModelDoc(ModelVocabularies.Core, "name", "Name for an entity"))

  override val key: Field = Name

  /** These Iris are always going to be domain classes URIs. No any class can be added to the domain.
    *
    * They are mapped from the allowedTargets property of an annotationType in RAML.
    */
  val Domain: Field =
    Field(Array(Iri), Rdfs + "domain", ModelDoc(ExternalModelVocabularies.Rdfs, "domain", "RDFS domain property"))

  /** A shape constraining the shape of the valid RDF graph for the property. It is parsed from the RAML type associated
    * to the annotationType.
    */
  val Schema: Field =
    Field(ShapeModel, Shapes + "schema", ModelDoc(ModelVocabularies.Shapes, "schema", "Schema for an entity"))

  val SerializationOrder: Field =
    Field(
      Int,
      Shapes + "serializationOrder",
      ModelDoc(
        ModelVocabularies.Shapes,
        "serializationOrder",
        "position in the set of properties for a shape used to serialize this property on the wire"
      )
    )

  val Repeatable: Field = Field(
    Bool,
    Core + "repeatable",
    ModelDoc(
      ModelVocabularies.Core,
      "repeatable",
      "Indicates if a Domain Element can define more than 1 Domain Extension defined by this Custom Domain Property"
    )
  )

  override val fields: List[Field] =
    List(Domain, Schema, Name, Description, SerializationOrder, Repeatable) ++ LinkableElementModel.fields ++ DomainElementModel.fields

  override val `type`: List[ValueType] = Document + "DomainProperty" :: Rdf + "Property" :: DomainElementModel.`type`

  override def modelInstance: CustomDomainProperty = CustomDomainProperty()

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.AmlDoc,
    "CustomDomainProperty",
    "Definition of an extension to the domain model defined directly by a user in the RAML/OpenAPI document.\nThis can be achieved by using an annotationType in RAML. In OpenAPI thy don't need to\n      be declared, they can just be used.\n      This should be mapped to new RDF properties declared directly in the main document or module.\n      Contrast this extension mechanism with the creation of a propertyTerm in a vocabulary, a more\nre-usable and generic way of achieving the same functionality.\nIt can be validated using a SHACL shape"
  )
}
