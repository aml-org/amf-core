package amf.plugins.document.webapi.resolution.pipelines.compatibility.oas

import amf.core.parser.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.stages.ResolutionStage
import amf.plugins.domain.webapi.metamodel.{OperationModel, ResponseModel}
import amf.plugins.domain.webapi.models.{Operation, Response}

class MandatoryResponses()(override implicit val errorHandler: ErrorHandler) extends ResolutionStage {

  override def resolve[T <: BaseUnit](model: T): T = {
    try {
      model.findByType(OperationModel.`type`.head.iri()).foreach {
        case operation: Operation =>
          if (operation.responses.isEmpty) {
            operation.withResponses(Seq(Response().withName("200").withStatusCode("200").withDescription("")))
          }
      }
      model
    } catch {
      case _: Exception => model
    }
  }

}
