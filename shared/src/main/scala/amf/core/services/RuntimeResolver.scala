package amf.core.services

import amf.client.errorhandling.DefaultErrorHandler
import amf.client.remod.amfcore.resolution.PipelineName
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.registries.AMFPluginsRegistry
import amf.core.resolution.pipelines.TransformationPipelineRunner
import amf.plugins.features.validation.CoreValidations.ResolutionValidation

object RuntimeResolver {

  def resolve(vendor: String, unit: BaseUnit, pipelineId: String): BaseUnit =
    resolve(vendor, unit, pipelineId, DefaultErrorHandler())

  /**
    * interface used by amf service
    */
  def resolve(vendor: String, unit: BaseUnit, pipelineId: String, errorHandler: AMFErrorHandler): BaseUnit = {
    val config    = AMFPluginsRegistry.obtainStaticConfig()
    val pipelines = config.registry.transformationPipelines
    val pipeline  = pipelines.get(PipelineName.from(vendor, pipelineId))

    pipeline match {
      case Some(pipeline) =>
        val runner = TransformationPipelineRunner(errorHandler, config.listeners.toList)
        runner.run(unit, pipeline)
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
