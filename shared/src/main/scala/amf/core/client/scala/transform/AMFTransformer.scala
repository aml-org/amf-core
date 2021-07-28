package amf.core.client.scala.transform

import amf.core.client.common.transform.{PipelineId, PipelineName}
import amf.core.client.scala.{AMFGraphConfiguration, AMFResult}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.Amf
import amf.core.internal.validation.CoreValidations.ResolutionValidation

object AMFTransformer {

  def transform(unit: BaseUnit, conf: AMFGraphConfiguration): AMFResult = {
    transform(unit, PipelineId.Default, conf)
  }

  def transform(unit: BaseUnit, pipelineId: String, conf: AMFGraphConfiguration): AMFResult = {
    val pipelines = conf.registry.transformationPipelines
    val pipeline  = pipelines.get(pipelineId)
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
            s"Cannot find transformation pipeline with name $pipelineId",
            unit.position(),
            unit.location()
        )
        unit
    }
    AMFResult(resolved, handler.getResults)
  }

}
