package amf.core.internal.annotations.serializable

import amf.core.internal.annotations._
import amf.core.client.scala.model.domain.AnnotationGraphLoader

private[amf] object CoreSerializableAnnotations extends SerializableAnnotations {

  override val annotations: Map[String, AnnotationGraphLoader] = Map(
      "lexical"              -> LexicalInformation,
      "host-lexical"         -> HostLexicalInformation,
      "base-path-lexical"    -> BasePathLexicalInformation,
      "source-vendor"        -> SourceSpec,
      "single-value-array"   -> SingleValueArray,
      "aliases-array"        -> Aliases,
      "synthesized-field"    -> SynthesizedField,
      "default-node"         -> DefaultNode,
      "data-node-properties" -> DataNodePropertiesAnnotations,
      "resolved-link"        -> ResolvedLinkAnnotation,
      "null-security"        -> NullSecurity
  )

}
