package amf.core.internal.metamodel.domain.extensions

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.{DataNodeModel, DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.client.scala.model.domain.extensions.ShapeExtension
import amf.core.client.scala.vocabulary.Namespace.{Document, ApiContract}
import amf.core.client.scala.vocabulary.ValueType

object ShapeExtensionModel extends DomainElementModel {
  val DefinedBy = Field(
      CustomDomainPropertyModel,
      Document + "definedBy",
      ModelDoc(ModelVocabularies.AmlDoc, "definedBy", "Definition for the extended entity")
  )
  val Extension = Field(
      DataNodeModel,
      Document + "extension",
      ModelDoc(ModelVocabularies.AmlDoc, "extension", "Data structure associated to the extension")
  )

  override val fields: List[Field] = List(DefinedBy, Extension) ++ DomainElementModel.fields

  override val `type`: List[ValueType] = ApiContract + "ShapeExtension" :: DomainElementModel.`type`

  override def modelInstance = ShapeExtension()

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.ApiContract,
      "ShapeExtension",
      "Custom extensions for a data shape definition inside an API definition"
  )
}
