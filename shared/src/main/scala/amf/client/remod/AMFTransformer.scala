package amf.client.remod

import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.TransformationPipelineRunner
import amf.core.validation.AMFValidationReport
import amf.plugins.features.validation.CoreValidations.ResolutionValidation

object AMFTransformer {

  def transform(unit: BaseUnit, configuration: AMFGraphConfiguration): AMFResult = ???

  def transform(unit: BaseUnit, pipelineName: String, configuration: AMFGraphConfiguration): AMFResult = {
    val pipelines = configuration.registry.transformationPipelines
    val pipeline  = pipelines.get(pipelineName)
    val handler   = configuration.errorHandlerProvider.errorHandler()
    val resolved = pipeline match {
      case Some(pipeline) =>
        val runner = TransformationPipelineRunner(handler, configuration.listeners.toList)
        runner.run(unit, pipeline)
      case None =>
        handler.violation(
            ResolutionValidation,
            unit.id,
            None,
            s"Cannot find transformation pipeline with name $pipelineName",
            unit.position(),
            unit.location()
        )
        unit
    }
    AMFResult(resolved, AMFValidationReport.forModel(resolved, handler.getResults))
  }

}
