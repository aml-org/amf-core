package amf.client.remod

import amf.core.AMFSerializer
import amf.core.model.document.BaseUnit
import amf.core.remote.Vendor.AMF
import org.yaml.model.YDocument

import scala.concurrent.Future

object AMFRenderer {

  def render(bu: BaseUnit, env: AMFGraphConfiguration): Future[String] = render(bu, AMF.mediaType, env)

  def renderAST(bu: BaseUnit, env: AMFGraphConfiguration): YDocument = ???

  /**
    *
    * @param bu
    * @param target media type which specifies a vendor, and optionally a syntax.
    * @param env
    * @return
    */
  def render(bu: BaseUnit, mediaType: String, env: AMFGraphConfiguration): Future[String] =
    new AMFSerializer(bu, mediaType, env.renderConfiguration).renderToString(env.getExecutionContext)

  def renderAST(bu: BaseUnit, mediaType: String, env: AMFGraphConfiguration): YDocument = ???

}
