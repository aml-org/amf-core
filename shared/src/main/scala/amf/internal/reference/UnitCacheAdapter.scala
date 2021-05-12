package amf.internal.reference

import amf.client.convert.CoreClientConverters._
import amf.client.reference.{UnitCache => ClientUnitCache}

import scala.concurrent.{ExecutionContext, Future}

/** Adapts a client ReferenceResolver to an internal one. */
case class UnitCacheAdapter(private[amf] val adaptee: ClientUnitCache)(implicit executionContext: ExecutionContext)
    extends UnitCache {

  override def fetch(url: String): Future[CachedReference] = adaptee.fetch(url).asInternal
}
