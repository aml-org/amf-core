package amf.core.resolution.stages

import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit

trait TransformationStep {
  def transform[T <: BaseUnit](model: T, errorHandler: ErrorHandler): T
}
