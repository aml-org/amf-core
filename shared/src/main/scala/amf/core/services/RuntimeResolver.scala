package amf.core.services

import amf.client.remod.amfcore.resolution.PipelineName
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.registries.AMFPluginsRegistry
import amf.plugins.features.validation.CoreValidations.ResolutionValidation

object RuntimeResolver {

  def resolve(vendor: String, unit: BaseUnit, pipelineId: String): BaseUnit =
    resolve(vendor, unit, pipelineId, unit.errorHandler())

  /**
    * interface used by amf service
    */
  def resolve(vendor: String, unit: BaseUnit, pipelineId: String, errorHandler: ErrorHandler): BaseUnit = {
    val pipelines = AMFPluginsRegistry.obtainStaticConfig().registry.transformationPipelines
    val pipeline  = pipelines.get(PipelineName.from(vendor, pipelineId))

    pipeline match {
      case Some(pipeline) => pipeline.transform(unit, errorHandler)
      case None =>
        errorHandler.violation(
            ResolutionValidation,
            unit.id,
            None,
            s"Cannot find domain plugin for vendor $vendor and pipeline $pipelineId to resolve unit ${unit.location()}",
            unit.position(),
            unit.location()
        )
        unit
    }
  }
}
