package amf.core.resolution.pipelines

import amf.core.benchmark.ExecutionLog
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.stages.TransformationStep

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

trait TransformationPipeline {

  val name: String

  def steps: Seq[TransformationStep]

  def transform[T <: BaseUnit](model: T, errorHandler: ErrorHandler): T = {
    ExecutionLog.log(s"${this.getClass.getName}#resolve: resolving ${model.location().getOrElse("")}")
    var m = model
    steps.foreach { s =>
      m = step(m, s, errorHandler)
    }
    // TODO: should be unit metadata
    m.resolved = true
    ExecutionLog.log(s"${this.getClass.getName}#resolve: resolved model ${m.location().getOrElse("")}")
    m
  }

  protected def step[T <: BaseUnit](unit: T, step: TransformationStep, errorHandler: ErrorHandler): T = {
    ExecutionLog.log(s"ResolutionPipeline#step: applying resolution stage ${step.getClass.getName}")
    val resolved = step.apply(unit, errorHandler)
    ExecutionLog.log(s"ResolutionPipeline#step: finished applying stage ${step.getClass.getName}")
    resolved
  }
}

@JSExportTopLevel("ResolutionPipeline")
@JSExportAll
object TransformationPipeline {
  val DEFAULT_PIPELINE       = "default"
  val EDITING_PIPELINE       = "editing"
  val COMPATIBILITY_PIPELINE = "compatibility"
  val CACHE_PIPELINE         = "cache"
}
