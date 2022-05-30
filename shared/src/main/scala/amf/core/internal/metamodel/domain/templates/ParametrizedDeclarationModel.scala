package amf.core.internal.metamodel.domain.templates

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Array
import amf.core.internal.metamodel.domain.common.NameFieldSchema
import amf.core.internal.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.client.scala.vocabulary.Namespace.Document
import amf.core.client.scala.vocabulary.ValueType

trait ParametrizedDeclarationModel extends DomainElementModel with KeyField with NameFieldSchema {

  val Target = Field(
      AbstractDeclarationModel,
      Document + "target",
      ModelDoc(ModelVocabularies.AmlDoc, "target", "Target node for the parameter")
  )

  val Variables = Field(
      Array(VariableValueModel),
      Document + "variable",
      ModelDoc(
          ModelVocabularies.AmlDoc,
          "variable",
          "Variables to be replaced in the graph template introduced by an AbstractDeclaration"
      )
  )

  override val key: Field = Name

  override def fields: List[Field] = List(Name, Target, Variables) ++ DomainElementModel.fields
}

object ParametrizedDeclarationModel extends ParametrizedDeclarationModel {
  override val `type`: List[ValueType] = Document + "ParametrizedDeclaration" :: DomainElementModel.`type`

  override def modelInstance =
    throw new Exception("ParametrizedDeclaration is abstract and cannot be instantiated by default")

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.AmlDoc,
      "ParametrizedDeclaration",
      "Generic graph template supporting variables that can be transformed into a domain element"
  )
}
