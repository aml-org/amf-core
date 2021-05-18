package amf.client.exported.transform

import amf.client.model.document.BaseUnit
import amf.client.resolve.ClientErrorHandler

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait TransformationStep {
  def transform[T <: BaseUnit](model: T, errorHandler: ClientErrorHandler): T
}
