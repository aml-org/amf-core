package amf.client.remod

import amf.core.model.document.BaseUnit
import org.yaml.model.YDocument

object AMFRenderer {

  def render(bu: BaseUnit, env: AMFGraphConfiguration): String = ???

  def renderAST(bu: BaseUnit, env: AMFGraphConfiguration): YDocument = ???

  /**
    *
    * @param bu
    * @param target media type which specifies a vendor, and optionally a syntax.
    * @param env
    * @return
    */
  def render(bu: BaseUnit, mediaType: String, env: AMFGraphConfiguration): String = ???

  def renderAST(bu: BaseUnit, mediaType: String, env: AMFGraphConfiguration): YDocument = ???

}
