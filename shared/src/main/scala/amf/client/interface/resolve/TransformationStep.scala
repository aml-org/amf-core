package amf.client.interface.resolve

import amf.client.model.document.BaseUnit
import amf.client.resolve.ClientErrorHandler

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait TransformationStep {
  def transform(model: BaseUnit, errorHandler: ClientErrorHandler): BaseUnit
}
