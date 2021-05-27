package amf.core.resolution.stages

import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit

trait TransformationStep {
  val id: String = this.getClass.getSimpleName
  def transform(model: BaseUnit, errorHandler: ErrorHandler): BaseUnit
}
