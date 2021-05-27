package amf.core.metamodel.document

import amf.core.metamodel.Type.{Array, Iri, Str}
import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.metamodel.domain.ModelVocabularies.AmlDoc
import amf.core.metamodel.{Field, ModelDefaultBuilder, Obj}
import amf.core.model.document.AmfProcessingData
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.core.vocabulary.Namespace.Meta

object AmfProcessingDataModel extends Obj with ModelDefaultBuilder {

  val Transformations: Field = Field(
      Array(Str),
      Meta + "unitTransformations",
      ModelDoc(AmlDoc, "unitTransformations", "Indicates the transformations that the Unit has gone through"))

  val ModelVersion: Field =
    Field(Str, Meta + "version", ModelDoc(AmlDoc, "version", "Version of the current model"))

  val SourceVendor: Field =
    Field(Str, Meta + "sourceVendor", ModelDoc(AmlDoc, "sourceVendor", "Vendor used to parse the Unit"))

  // TODO: add description
  val GraphDependencies: Field = Field(Array(Iri), Meta + "graphDependencies", ModelDoc(AmlDoc, "graphDependencies"))

  override val `type`: List[ValueType] = Namespace.Meta + "AmfProcessingData" :: Nil

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.AmlDoc,
      "AmfProcessingData",
      "Provides information on how a Unit was processed"
  )

  override def fields: List[Field] = List(ModelVersion, SourceVendor, Transformations, GraphDependencies)

  override def modelInstance: AmfObject = AmfProcessingData()
}
