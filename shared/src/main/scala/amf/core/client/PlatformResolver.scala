package amf.core.client

import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.services.RuntimeResolver

abstract class PlatformResolver(vendor: String) {

  def resolve(unit: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit =
    RuntimeResolver.resolve(vendor, unit, TransformationPipeline.DEFAULT_PIPELINE, errorHandler)
  def resolve(unit: BaseUnit, pipelineId: String, errorHandler: AMFErrorHandler): BaseUnit =
    RuntimeResolver.resolve(vendor, unit, pipelineId, errorHandler)
}
