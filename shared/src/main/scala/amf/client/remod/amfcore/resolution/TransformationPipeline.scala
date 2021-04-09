package amf.client.remod.amfcore.resolution

import amf.core.resolution.pipelines.ResolutionPipeline

private[amf] object TransformationPipeline {
  val DEFAULT: String = ResolutionPipeline.DEFAULT_PIPELINE
  val EDITING: String = ResolutionPipeline.EDITING_PIPELINE
  val CACHE: String   = ResolutionPipeline.CACHE_PIPELINE

  val OAS_TO_RAML10 = "oas-to-raml-10"
  val RAML_TO_OAS20 = "raml-to-oas-20"
  val RAML_TO_OAS30 = "raml-to-oas-30"
}
