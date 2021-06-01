package amf.core.resolution.pipelines.elements

import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.domain.DomainElement
import amf.core.resolution.stages.elements.resolution.ElementStageTransformer

abstract class ElementTransformationPipeline[T <: DomainElement](element: T, errorHandler: AMFErrorHandler) {

  val steps: Seq[ElementStageTransformer[T]]

  final def resolve(): T = {
    var result: T = element
    steps.foreach { s =>
      s.transform(result).foreach(result = _)
    }
    result
  }

}
