package amf.core.resolution.stages

import amf.core.annotations.References
import amf.core.errorhandling.AMFErrorHandler
import amf.core.metamodel.document.BaseUnitModel
import amf.core.model.document.{BaseUnit, Fragment, Module}

class CleanReferencesStage() extends TransformationStep {
  override def transform(model: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit = {
    persistReferenceShapes(model)

    model.fields.removeField(BaseUnitModel.References)
    model
  }

  private def persistReferenceShapes[T <: BaseUnit](model: T): Unit = {
    val referenceShapes = model.references.collect {
      case fragment: Fragment => Option(fragment.encodes).map(_.id).toList
      case module: Module     => module.declares.map(_.id)
    }.flatten
    model.annotations += References(referenceShapes)
  }
}
