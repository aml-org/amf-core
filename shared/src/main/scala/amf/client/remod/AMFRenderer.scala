package amf.client.remod

import amf.core.AMFSerializer
import amf.core.model.document.BaseUnit
import amf.core.remote.Vendor.AMF
import org.yaml.model.YDocument

import scala.concurrent.Future

object AMFRenderer {

  def render(bu: BaseUnit, configuration: AMFGraphConfiguration): Future[String] = render(bu, AMF.mediaType, configuration)

  def renderAST(bu: BaseUnit, configuration: AMFGraphConfiguration): YDocument = ???

  /**
    *
    * @param bu
    * @param target media type which specifies a vendor, and optionally a syntax.
    * @param env
    * @return
    */
  def render(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): Future[String] =
    new AMFSerializer(bu, mediaType, configuration.renderConfiguration).renderToString(configuration.getExecutionContext)

  def renderAST(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): YDocument = ???

}
