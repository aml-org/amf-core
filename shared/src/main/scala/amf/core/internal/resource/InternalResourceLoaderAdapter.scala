package amf.core.internal.resource

import amf.core.client.common.remote.Content
import amf.core.client.platform.resource.{ResourceLoader => ClientResourceLoader}
import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.convert.CoreClientConverters._

import scala.concurrent.{ExecutionContext, Future}

/** Adapts a client ResourceLoader to an internal one. */
case class InternalResourceLoaderAdapter(private[amf] val adaptee: ClientResourceLoader) extends ResourceLoader {

  override def fetch(resource: String)(implicit ec: ExecutionContext): Future[Content] =
    adaptee.fetch(resource, ec).asInternal

  override def accepts(resource: String): Boolean = adaptee.accepts(resource)
}

case class ClientResourceLoaderAdapter(private[amf] val adaptee: ResourceLoader) extends ClientResourceLoader {

  override def fetch(resource: String, ec: ExecutionContext): ClientFuture[Content] = {
    implicit val implicitEc: ExecutionContext = ec
    adaptee.fetch(resource).asClient
  }

  override def accepts(resource: String): Boolean = adaptee.accepts(resource)
}
