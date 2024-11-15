package amf.core.internal.metamodel.domain

import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Type.{Bool, Iri, Str}
import amf.core.internal.metamodel.{Field, Obj}

/** Reification of a link between elements in the model. Used when we want to capture the structure of the source
  * document in the graph itself. Linkable elements are just replaced by regular links after resolution.
  */
trait LinkableElementModel extends Obj {

  /** Uri of the linked element
    */
  val TargetId: Field = Field(
    Iri,
    Namespace.Document + "link-target",
    ModelDoc(ModelVocabularies.AmlDoc, "linkTarget", "URI of the linked element")
  )

  // Never serialise this
  val Target: Field = Field(
    DomainElementModel,
    Namespace.Document + "effective-target",
    ModelDoc(ModelVocabularies.AmlDoc, "effectiveTarget", "URI of the final element in a chain of linked elements")
  )

  /** Label for the type of link
    */
  val Label: Field = Field(
    Str,
    Namespace.Document + "link-label",
    ModelDoc(ModelVocabularies.AmlDoc, "linkLabel", "Label for the type of link")
  )

  // TODO: maybe remove this? What is this used for?
  val SupportsRecursion: Field = Field(
    Bool,
    Namespace.Document + "recursive",
    ModelDoc(
      ModelVocabularies.AmlDoc,
      "supportsRecursion",
      "Indication taht this kind of linkable element can support recursive links"
    )
  )

  val RefSummary: Field = Field(
    Str,
    Namespace.Document + "ref-summary",
    ModelDoc(
      ModelVocabularies.AmlDoc,
      "ref-summary",
      "temporary holder for a reference summary field to override the summary in the referenced object"
    )
  )

  val RefDescription: Field = Field(
    Str,
    Namespace.Document + "ref-description",
    ModelDoc(
      ModelVocabularies.AmlDoc,
      "ref-description",
      "temporary holder for a reference description field to override the description in the referenced object"
    )
  )
}

object LinkableElementModel extends LinkableElementModel {

  // 'Static' values, we know the element schema before parsing
  // If the domain element is dynamic, the value from the model,
  // not the meta-model, should be retrieved instead

  override val `type`: List[ValueType] = List(Namespace.Document + "Linkable")

  override val fields: List[Field] = List(TargetId, Label, SupportsRecursion)

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.AmlDoc,
    "LinkableElement",
    "Reification of a link between elements in the model. Used when we want to capture the structure of the source document\nin the graph itself. Linkable elements are just replaced by regular links after resolution."
  )
}
