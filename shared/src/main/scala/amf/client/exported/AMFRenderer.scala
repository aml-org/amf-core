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

  def render(bu: BaseUnit, env: AMFGraphConfiguration): ClientFuture[String] = {
    implicit val executionContext: ExecutionContext = env.getExecutionContext
    InternalAMFRenderer.render(bu, env).asClient
  }

  def render(bu: BaseUnit, mediaType: String, env: AMFGraphConfiguration): ClientFuture[String] = {
    implicit val executionContext: ExecutionContext = env.getExecutionContext
    InternalAMFRenderer.render(bu, mediaType, env).asClient
  }

}
