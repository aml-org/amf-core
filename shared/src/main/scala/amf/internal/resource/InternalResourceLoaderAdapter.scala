package amf.internal.resource

import amf.client.remote.Content
import amf.client.resource.{ResourceLoader => ClientResourceLoader}
import amf.client.convert.CoreClientConverters._

import scala.concurrent.{ExecutionContext, Future}

/** Adapts a client ResourceLoader to an internal one. */
case class InternalResourceLoaderAdapter(private[amf] val adaptee: ClientResourceLoader)(
    implicit executionContext: ExecutionContext)
    extends ResourceLoader {

  override def fetch(resource: String): Future[Content] = adaptee.fetch(resource).asInternal

  override def accepts(resource: String): Boolean = adaptee.accepts(resource)
}

case class ClientResourceLoaderAdapter(private[amf] val adaptee: ResourceLoader)(
  implicit executionContext: ExecutionContext)
  extends ClientResourceLoader {

  override def fetch(resource: String): ClientFuture[Content] = adaptee.fetch(resource).asClient

  override def accepts(resource: String): Boolean = adaptee.accepts(resource)
}
