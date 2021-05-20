package amf.client.exported

import amf.client.model.document.BaseUnit
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.remod.{AMFRenderer => InternalAMFRenderer}
import amf.client.convert.CoreClientConverters._

@JSExportAll
@JSExportTopLevel("AMFRenderer")
object AMFRenderer {
  // TODO: return AMFRenderResult?

  def render(bu: BaseUnit, configuration: AMFGraphConfiguration): String =
    InternalAMFRenderer.render(bu, configuration)

  def render(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): String =
    InternalAMFRenderer.render(bu, mediaType, configuration)

}
