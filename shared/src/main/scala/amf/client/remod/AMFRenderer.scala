package amf.client.remod

import amf.core.AMFSerializer
import amf.core.model.document.BaseUnit
import org.yaml.model.YDocument

import scala.concurrent.Future

object AMFRenderer {

  def render(bu: BaseUnit, configuration: AMFGraphConfiguration): Future[String] = render(bu, configuration)

  // TODO: ARM - Implement!
  def renderAST(bu: BaseUnit, configuration: AMFGraphConfiguration): YDocument = ???

  def render(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): Future[String] =
    new AMFSerializer(bu, mediaType, configuration.renderConfiguration)
      .renderToString(configuration.getExecutionContext)

  // TODO: ARM - Implement!
  def renderAST(bu: BaseUnit, mediaType: String, configuration: AMFGraphConfiguration): YDocument = ???
}
