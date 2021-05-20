package amf.client.remod

import amf.core.model.document.BaseUnit

object AMFTransformer {

  def transform(unit: BaseUnit, configuration: AMFGraphConfiguration): AMFResult = ???

  def transform(unit: BaseUnit, pipelineName: String, configuration: AMFGraphConfiguration): AMFResult = ???
//  {
//    val pipelines = conf.registry.transformationPipelines
//    val pipeline  = pipelines.get(pipelineName)
//
//    val handler = DefaultErrorHandler() // what about the error handler provider?
//    pipeline match {
//      case Some(pipeline) =>
//        val runner = TransformationPipelineRunner(handler, conf.listeners.toList)
//        val result = runner.run(unit, pipeline)
//        AMFResult(result, handler.getErrors)
//      case None =>
//        handler.violation(
//          ResolutionValidation,
//          unit.id,
//          None,
//          s"Cannot find transformation pipeline with name $pipelineName",
//          unit.position(),
//          unit.location()
//        )
//        unit
//    }
//  }

}
