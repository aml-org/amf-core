package amf.core.internal.metamodel.domain

import amf.core.internal.metamodel.Type.{Iri, Str, Bool}
import amf.core.internal.metamodel.{Field, Obj}
import amf.core.client.scala.vocabulary.{Namespace, ValueType}

/** Reification of a link between elements in the model. Used when we want to capture the structure of the source
  * document in the graph itself. Linkable elements are just replaced by regular links after resolution.
  */
trait LinkableElementModel extends Obj {

  /** Uri of the linked element
    */
  val TargetId = Field(
      Iri,
      Namespace.Document + "link-target",
      ModelDoc(ModelVocabularies.AmlDoc, "linkTarget", "URI of the linked element")
  )

  // Never serialise this
  val Target = Field(
      DomainElementModel,
      Namespace.Document + "effective-target",
      ModelDoc(ModelVocabularies.AmlDoc, "effectiveTarget", "URI of the final element in a chain of linked elements")
  )

  /** Label for the type of link
    */
  val Label = Field(
      Str,
      Namespace.Document + "link-label",
      ModelDoc(ModelVocabularies.AmlDoc, "linkLabel", "Label for the type of link")
  )

  val SupportsRecursion = Field(
      Bool,
      Namespace.Document + "recursive",
      ModelDoc(
          ModelVocabularies.AmlDoc,
          "supportsRecursion",
          "Indication taht this kind of linkable element can support recursive links"
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
