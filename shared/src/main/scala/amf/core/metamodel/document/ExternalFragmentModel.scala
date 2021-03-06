package amf.core.metamodel.document

import amf.core.metamodel.Field
import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.model.document.ExternalFragment
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.Namespace.Document
import amf.core.vocabulary.ValueType

/**
  * A fragment with including raw information that cannot be semantically processed, the information is encoded as a raw opaque textual description.
  */
object ExternalFragmentModel extends FragmentModel {
  override val fields: List[Field] = FragmentModel.fields

  override val `type`: List[ValueType] = List(Document + "ExternalFragment") ++ FragmentModel.`type`

  override def modelInstance: AmfObject = ExternalFragment()

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.AmlDoc,
      "ExternalFragment",
      "Fragment encoding an external entity"
  )
}
