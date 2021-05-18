package amf.core.resolution.stages

import amf.core.errorhandling.AMFErrorHandler
import amf.core.metamodel.domain.ExternalSourceElementModel
import amf.core.model.document.BaseUnit
import amf.core.model.domain.{DomainElement, ExternalSourceElement}
import amf.core.resolution.stages.selectors.{ExternalSourceElementSelector, KnownElementIdSelector}

import scala.collection.mutable

class ExternalSourceRemovalStage(val visited: mutable.Set[String] = mutable.Set()) extends TransformationStep {

  override def transform(model: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit = {
    val knownIdSelector = new KnownElementIdSelector(visited)
    model.transform(knownIdSelector || ExternalSourceElementSelector, transformation)(errorHandler)
  }

  private def transformation(element: DomainElement, cycle: Boolean): Option[DomainElement] = {
    element match {
      case ex: ExternalSourceElement =>
        ex.fields.removeField(ExternalSourceElementModel.ReferenceId)
        Some(ex)
      case _ => Some(element)
    }
  }
}
