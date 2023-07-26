package amf.core.client.scala.adoption

import amf.core.client.scala.model.domain.AmfObject

trait IdAdopterProvider {
  def idAdopter(initialId: String): IdAdopter

  def idAdopter(initialId: String, idMaker: IdMaker): IdAdopter

  private[amf] def adoptFromRoot(initialElem: AmfObject, initialId: String): Unit =
    idAdopter(initialId).adoptFromRoot(initialElem)
}

class DefaultIdAdopterProvider extends IdAdopterProvider {
  override def idAdopter(initialId: String) = new IdAdopter(initialId) // Shall we expose the idmaker?
  override def idAdopter(initialId: String, idMaker: IdMaker) =
    new IdAdopter(initialId, idMaker) // Shall we expose the idmaker?
}
