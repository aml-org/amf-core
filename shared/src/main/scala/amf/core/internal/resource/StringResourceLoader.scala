package amf.core.internal.resource

import amf.core.client.common.remote.Content
import amf.core.client.scala.resource.ResourceLoader

import scala.concurrent.{ExecutionContext, Future}

case class StringResourceLoader(url: String, content: String) extends ResourceLoader {
  private val _content = new Content(content, url)

  /** Fetch specified resource and return associated content. Resource should have benn previously accepted. */
  override def fetch(resource: String)(implicit ec: ExecutionContext): Future[Content] = Future.successful(_content)

  /** Accepts specified resource. */
  override def accepts(resource: String): Boolean = resource == url
}
