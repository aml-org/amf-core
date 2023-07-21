package amf.core.client.platform.adoption

import amf.core.client.platform.model.AmfObjectWrapper
import amf.core.client.scala.adoption.{IdAdopter => InternalIdAdopter}
import amf.core.internal.convert.CoreClientConverters._

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class IdAdopter(private[amf] val _internal: InternalIdAdopter) {

  def this(initialId: String) = this(new InternalIdAdopter(initialId))
  def this(initialId: String, idMaker: IdMaker) = this(new InternalIdAdopter(initialId, idMaker))

  def adoptFromRoot(initialElem: AmfObjectWrapper): Unit     = _internal.adoptFromRoot(initialElem)
  def adoptFromRelative(initialElem: AmfObjectWrapper): Unit = _internal.adoptFromRoot(initialElem)
}
