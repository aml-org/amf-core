package amf.core.annotations.serializable

import amf.core.annotations._
import amf.core.model.domain.AnnotationGraphLoader

private[amf] object CoreSerializableAnnotations extends SerializableAnnotations {

  override val annotations: Map[String, AnnotationGraphLoader] = Map(
      "lexical"              -> LexicalInformation,
      "host-lexical"         -> HostLexicalInformation,
      "base-path-lexical"    -> BasePathLexicalInformation,
      "source-vendor"        -> SourceVendor,
      "single-value-array"   -> SingleValueArray,
      "aliases-array"        -> Aliases,
      "synthesized-field"    -> SynthesizedField,
      "default-node"         -> DefaultNode,
      "data-node-properties" -> DataNodePropertiesAnnotations,
      "resolved-link"        -> ResolvedLinkAnnotation,
      "null-security"        -> NullSecurity
  )

}
