package amf.client.exported

import amf.client.model.document.BaseUnit

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.remod.{AMFRenderer => InternalAMFRenderer}
import amf.client.convert.CoreClientConverters._

import scala.concurrent.ExecutionContext

@JSExportAll
@JSExportTopLevel("AMFRenderer")
object AMFRenderer {
  // TODO: return AMFRenderResult?

  def render(bu: BaseUnit, configuration: AMFGraphConfiguration): ClientFuture[String] = {
    implicit val executionContext: ExecutionContext = configuration.getExecutionContext
    InternalAMFRenderer.render(bu, configuration).asClient
  }

  def render(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): ClientFuture[String] = {
    implicit val executionContext: ExecutionContext = configuration.getExecutionContext
    InternalAMFRenderer.render(bu, mediaType, configuration).asClient
  }

}
