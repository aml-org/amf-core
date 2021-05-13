package amf.client.interface

import amf.client.model.document.BaseUnit
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.remod.{AMFRenderer => InternalAMFRenderer}
import amf.client.convert.CoreClientConverters._

@JSExportAll
@JSExportTopLevel("AMFRenderer")
object AMFRenderer {
  // TODO: return AMFRenderResult?

  def render(bu: BaseUnit, env: AMFGraphConfiguration): String = InternalAMFRenderer.render(bu, env)

  def render(bu: BaseUnit, mediaType: String, env: AMFGraphConfiguration): String =
    InternalAMFRenderer.render(bu, mediaType, env)

}
