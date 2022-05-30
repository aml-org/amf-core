package amf.core.internal.metamodel.domain

import amf.core.internal.metamodel.{Field, domain}
import amf.core.internal.metamodel.Type.Str
import amf.core.client.scala.model.domain.ExternalDomainElement
import amf.core.client.scala.vocabulary.Namespace.{Document, Core}
import amf.core.client.scala.vocabulary.ValueType

/** Domain element containing foreign information that cannot be included into the model semantics
  */
object ExternalDomainElementModel extends DomainElementModel {

  /** Raw textual information that cannot be processed for the current model semantics.
    */
  val Raw = Field(
      Str,
      Document + "raw",
      ModelDoc(
          ModelVocabularies.AmlDoc,
          "raw",
          "Raw textual information that cannot be processed for the current model semantics."
      )
  )

  val MediaType = Field(
      Str,
      Core + "mediaType",
      ModelDoc(ModelVocabularies.Core, "mediaType", "Media type associated to the encoded fragment information")
  )

  override val fields: List[Field] = List(Raw, MediaType)

  override val `type`: List[ValueType] = Document + "ExternalDomainElement" :: DomainElementModel.`type`

  override def modelInstance = ExternalDomainElement()

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.AmlDoc,
      "ExternalDomainElement",
      "Domain element containing foreign information that cannot be included into the model semantics"
  )
}
