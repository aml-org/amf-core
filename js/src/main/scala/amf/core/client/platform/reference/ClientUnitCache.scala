package amf.core.client.platform.reference

import amf.core.internal.convert.CoreClientConverters
import amf.core.internal.convert.CoreClientConverters.ClientFuture

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@js.native
trait ClientUnitCache extends js.Object {

  /** Fetch specified reference and return associated [[CachedReference]]. Resource should have benn previously
    * accepted.
    */
  /** If the resource not exists, you should return a future failed with an ResourceNotFound exception. */
  def fetch(url: String): ClientFuture[CachedReference] = js.native

}

@JSExportAll
@JSExportTopLevel("ClientUnitCacheAdapter")
object ClientUnitCacheAdapter {
  def adapt(obj: ClientUnitCache): UnitCache = { (url: String) =>
    obj.fetch(url)
  }
}
