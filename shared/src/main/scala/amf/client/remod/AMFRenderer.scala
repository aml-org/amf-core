package amf.client.remod

import amf.core.model.document.BaseUnit
import org.yaml.model.YDocument

private[remod] object AMFRenderer {

  def render(bu: BaseUnit, env: AMFGraphConfiguration): String = ???

  def buildAST(bu: BaseUnit, env: AMFGraphConfiguration): YDocument = ???

  /**
    *
    * @param bu
    * @param target media type which specifies a vendor, and optionally a syntax.
    * @param env
    * @return
    */
  def render(bu: BaseUnit, mediaType: String, env: AMFGraphConfiguration): String = ???

  def buildAST(bu: BaseUnit, mediaType: String, env: AMFGraphConfiguration): YDocument = ???

}
