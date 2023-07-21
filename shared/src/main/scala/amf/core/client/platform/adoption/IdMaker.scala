package amf.core.client.platform.adoption

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait IdMaker {
  def makeId(parent: String, element: String): String
}
