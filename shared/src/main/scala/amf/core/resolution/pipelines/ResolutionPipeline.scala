package amf.core.resolution.pipelines

import amf.ProfileName
import amf.core.benchmark.ExecutionLog
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.stages.ResolutionStage

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

// TODO rename to TransformationPipeline
trait ResolutionPipeline {

  def steps(model: BaseUnit, sourceVendor: String)(implicit errorHandler: ErrorHandler): Seq[ResolutionStage]

  // sourceVendor is temporary to ensure no breaking changes.
  // should be accessed through the base unit directly when needed
  def transform[T <: BaseUnit](model: T, sourceVendor: String, errorHandler: ErrorHandler): T = {
    ExecutionLog.log(s"${this.getClass.getName}#resolve: resolving ${model.location().getOrElse("")}")
    var m = model
    steps(model, sourceVendor)(errorHandler).foreach { s =>
      m = step(m, s)
    }
    // TODO: should be unit metadata
    m.resolved = true
    ExecutionLog.log(s"${this.getClass.getName}#resolve: resolved model ${m.location().getOrElse("")}")
    m
  }

  protected def step[T <: BaseUnit](unit: T, stage: ResolutionStage): T = {
    ExecutionLog.log(s"ResolutionPipeline#step: applying resolution stage ${stage.getClass.getName}")
    val resolved = stage.resolve(unit)
    ExecutionLog.log(s"ResolutionPipeline#step: finished applying stage ${stage.getClass.getName}")
    resolved
  }
}

@JSExportTopLevel("ResolutionPipeline")
@JSExportAll
object ResolutionPipeline {
  val DEFAULT_PIPELINE       = "default"
  val EDITING_PIPELINE       = "editing"
  val COMPATIBILITY_PIPELINE = "compatibility"
  val CACHE_PIPELINE         = "cache"
}
