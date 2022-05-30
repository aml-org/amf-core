package amf.core.internal.metamodel.document

import amf.core.internal.metamodel.Type.Str
import amf.core.internal.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.internal.metamodel.{Field, Obj}
import amf.core.client.scala.vocabulary.Namespace.SourceMaps
import amf.core.client.scala.vocabulary.ValueType

/** Source Map Metadata
  *
  * SourceMaps include tags with syntax specific information obtained when parsing a particular specification syntax
  * like RAML or OpenAPI. It can be used to re-generate the document from the RDF model with a similar syntax
  */
object SourceMapModel extends Obj {

  val Element: Field = Field(
      Str,
      SourceMaps + "element",
      ModelDoc(ModelVocabularies.AmlDoc, "element", "Label indicating the type of source map information")
  )

  val Value: Field =
    Field(Str, SourceMaps + "value", ModelDoc(ModelVocabularies.AmlDoc, "value", "Value for the source map."))

  override val fields: List[Field] = Nil

  override val `type`: List[ValueType] = List(SourceMaps + "SourceMap")

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.AmlDoc,
      "SourceMap",
      "SourceMaps include tags with syntax specific information obtained when parsing a particular specification syntax like RAML or OpenAPI.\nIt can be used to re-generate the document from the RDF model with a similar syntax"
  )
}
