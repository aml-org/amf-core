package amf.client.remod

import amf.core.AMFSerializer
import amf.core.model.document.BaseUnit
import amf.core.remote.Vendor.AMF
import org.yaml.model.YDocument

import scala.concurrent.Future

object AMFRenderer {

  def render(bu: BaseUnit, configuration: AMFGraphConfiguration): String = render(bu, AMF.mediaType, configuration)

  def renderAST(bu: BaseUnit, configuration: AMFGraphConfiguration): YDocument = ???

  /**
    *
    * @param bu
    * @param target media type which specifies a vendor, and optionally a syntax.
    * @param env
    * @return
    */
  def render(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): String =
    new AMFSerializer(bu, mediaType, configuration.renderConfiguration).renderToString

  def renderAST(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): YDocument = ???

}
