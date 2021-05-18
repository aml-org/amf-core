package amf.core.resolution.stages

import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit

trait TransformationStep {
  def transform(model: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit
}
