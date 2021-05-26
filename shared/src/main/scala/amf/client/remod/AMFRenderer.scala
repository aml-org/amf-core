package amf.client.remod

import amf.core.model.document.BaseUnit
import org.yaml.model.YDocument

import scala.concurrent.Future

object AMFRenderer {

  def render(bu: BaseUnit, configuration: AMFGraphConfiguration): Future[String] = ???

  def renderAST(bu: BaseUnit, configuration: AMFGraphConfiguration): YDocument = ???

  def render(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): Future[String] = ???

  def renderAST(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): YDocument = ???
}
