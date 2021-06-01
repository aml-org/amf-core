package amf.client.exported

import amf.client.model.document.BaseUnit

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.remod.{AMFRenderer => InternalAMFRenderer}
import amf.client.convert.CoreClientConverters._

import scala.concurrent.ExecutionContext

@JSExportAll
@JSExportTopLevel("AMFRenderer")
object AMFRenderer {

  def render(bu: BaseUnit, env: AMFGraphConfiguration): String = {
    implicit val executionContext: ExecutionContext = env.getExecutionContext
    InternalAMFRenderer.render(bu, env)
  }

  def render(bu: BaseUnit, mediaType: String, env: AMFGraphConfiguration): String = {
    implicit val executionContext: ExecutionContext = env.getExecutionContext
    InternalAMFRenderer.render(bu, mediaType, env)
  }

}
