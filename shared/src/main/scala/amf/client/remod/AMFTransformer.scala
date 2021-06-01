package amf.client.remod

import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.TransformationPipelineRunner
import amf.core.validation.AMFValidationReport
import amf.plugins.features.validation.CoreValidations.ResolutionValidation

object AMFTransformer {

  def transform(unit: BaseUnit, configuration: AMFGraphConfiguration): AMFResult = ???

  def transform(unit: BaseUnit, pipelineName: String, conf: AMFGraphConfiguration): AMFResult = {
    val pipelines = conf.registry.transformationPipelines
    val pipeline  = pipelines.get(pipelineName)
    val handler   = conf.errorHandlerProvider.errorHandler()
    val resolved = pipeline match {
      case Some(pipeline) =>
        val runner = TransformationPipelineRunner(handler, conf.listeners.toList)
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
    AMFResult(resolved, handler.getResults)
  }

}
