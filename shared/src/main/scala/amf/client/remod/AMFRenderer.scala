package amf.client.remod

import amf.core.model.document.BaseUnit
import org.yaml.model.YDocument

object AMFRenderer {

  def render(bu: BaseUnit, configuration: AMFGraphConfiguration): String = ???

  def renderAST(bu: BaseUnit, configuration: AMFGraphConfiguration): YDocument = ???

  def render(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): String = ???

  def renderAST(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): YDocument = ???

}
