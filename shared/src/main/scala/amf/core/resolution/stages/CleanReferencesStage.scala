package amf.core.resolution.stages

import amf.core.annotations.References
import amf.core.errorhandling.ErrorHandler
import amf.core.metamodel.document.BaseUnitModel
import amf.core.model.document.{BaseUnit, Fragment, Module}

class CleanReferencesStage() extends TransformationStep {
  override def transform[T <: BaseUnit](model: T, errorHandler: ErrorHandler): T = {
    persistReferenceShapes(model)

    model.fields.removeField(BaseUnitModel.References)
    model.asInstanceOf[T]
  }

  private def persistReferenceShapes[T <: BaseUnit](model: T): Unit = {
    val referenceShapes = model.references.collect {
      case fragment: Fragment => Option(fragment.encodes).map(_.id).toList
      case module: Module     => module.declares.map(_.id)
    }.flatten
    model.annotations += References(referenceShapes)
  }
}
