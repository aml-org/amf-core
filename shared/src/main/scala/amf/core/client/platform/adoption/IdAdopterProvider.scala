package amf.core.client.platform.adoption

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait IdAdopterProvider {
  def idAdopter(initialId: String): IdAdopter

  def idAdopter(initialId: String, idMaker: IdMaker): IdAdopter
}

class DefaultIdAdopterProvider extends IdAdopterProvider {
  override def idAdopter(initialId: String) = new IdAdopter(initialId)
  override def idAdopter(initialId: String, idMaker: IdMaker) =
    new IdAdopter(initialId, idMaker) // Shall we expose the idmaker?
}
