package amf.core.resolution.stages

import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit

trait ResolutionStage {
  def resolve[T <: BaseUnit](model: T, errorHandler: ErrorHandler): T
}
