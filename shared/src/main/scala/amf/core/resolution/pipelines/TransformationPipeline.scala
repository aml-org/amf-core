package amf.core.resolution.pipelines

import amf.client.interface.config.{AMFEvent, AMFEventListener}
import amf.client.remod.amfcore.config.{
  FinishedTransformationEvent,
  FinishedTransformationStepEvent,
  StartingTransformationEvent
}
import amf.core.benchmark.ExecutionLog
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.stages.TransformationStep

trait TransformationPipeline {
  val name: String
  def steps: Seq[TransformationStep]
}

// transformation pipelines can only run internally within amf.
private[amf] case class TransformationPipelineRunner(errorHandler: ErrorHandler,
                                                     listeners: Seq[AMFEventListener] = Nil) {

  private def notifyEvent(e: AMFEvent): Unit = listeners.foreach(_.notifyEvent(e))

  def run(model: BaseUnit, pipeline: TransformationPipeline): BaseUnit = {
    ExecutionLog.log(s"${this.getClass.getName}#resolve: resolving ${model.location().getOrElse("")}")
    notifyEvent(StartingTransformationEvent(pipeline))
    var m     = model
    val steps = pipeline.steps
    steps.zipWithIndex foreach {
      case (s, index) =>
        m = step(m, s, errorHandler)
        notifyEvent(FinishedTransformationStepEvent(s, index))
    }
    // TODO: should be unit metadata
    m.resolved = true
    notifyEvent(FinishedTransformationEvent(m))
    ExecutionLog.log(s"${this.getClass.getName}#resolve: resolved model ${m.location().getOrElse("")}")
    m
  }

  private def step(unit: BaseUnit, step: TransformationStep, errorHandler: ErrorHandler): BaseUnit = {
    ExecutionLog.log(s"ResolutionPipeline#step: applying resolution stage ${step.getClass.getName}")
    val resolved = step.transform(unit, errorHandler)
    ExecutionLog.log(s"ResolutionPipeline#step: finished applying stage ${step.getClass.getName}")
    resolved
  }
}

//TODO: this should be modified to include full pipeline names
object TransformationPipeline {
  val DEFAULT_PIPELINE       = "default"
  val EDITING_PIPELINE       = "editing"
  val COMPATIBILITY_PIPELINE = "compatibility"
  val CACHE_PIPELINE         = "cache"
}
