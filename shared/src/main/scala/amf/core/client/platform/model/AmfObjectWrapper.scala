package amf.core.client.platform.model

import amf.core.client.scala.model.domain.{AmfObject => InternalAmfObject}
import amf.core.internal.convert.CoreClientConverters._

import scala.scalajs.js.annotation.JSExportAll

/** Base class for all the native wrappers
  */
@JSExportAll
trait AmfObjectWrapper extends Annotable {
  private[amf] val _internal: InternalAmfObject

  override def annotations(): Annotations = _internal.annotations
  def findObjectFieldValue(iri: String): ClientOption[AmfObjectWrapper] = _internal.findObjectFieldValue(iri).asClient
}
