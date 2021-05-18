package amf.core.resolution.stages

import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.model.domain.{DomainElement, LinkNode}
import amf.core.resolution.stages.helpers.{LinkNodeResolver, ModelReferenceResolver}
import amf.core.resolution.stages.selectors.{KnownElementIdSelector, LinkNodeSelector, LinkSelector}

import scala.collection.mutable

class LinkNodeResolutionStage(keepEditingInfo: Boolean, val visited: mutable.Set[String] = mutable.Set())
    extends TransformationStep {

  var modelResolver: Option[ModelReferenceResolver] = None

  override def transform(model: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit = {
    this.modelResolver = Some(new ModelReferenceResolver(model))
    val knownIdSelector = new KnownElementIdSelector(visited)
    model.transform(knownIdSelector || LinkSelector || LinkNodeSelector, transformation)(errorHandler)
  }

  private def transformation(element: DomainElement, cycle: Boolean): Option[DomainElement] = {
    element match {
      case ln: LinkNode => LinkNodeResolver.resolveDynamicLink(ln, modelResolver, keepEditingInfo)
      case _            => Some(element)
    }
  }
}
