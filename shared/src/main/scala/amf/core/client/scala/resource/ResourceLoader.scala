package amf.core.client.scala.resource

import amf.core.client.common.remote.Content

import scala.concurrent.{ExecutionContext, Future}

trait ResourceLoader {

  /** Fetch specified resource and return associated content. Resource should have benn previously accepted. */
  def fetch(resource: String)(implicit ec: ExecutionContext): Future[Content]

  /** Accepts specified resource. */
  def accepts(resource: String): Boolean
}
