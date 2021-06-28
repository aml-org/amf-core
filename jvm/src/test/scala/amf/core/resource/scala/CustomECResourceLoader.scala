package amf.core.resource.scala

import amf.core.client.common.remote.Content
import amf.core.client.scala.resource.{LoaderWithExecutionContext, ResourceLoader}

import scala.concurrent.{ExecutionContext, Future}

case class CustomECResourceLoader(ec: ExecutionContext) extends ResourceLoader with LoaderWithExecutionContext {

  /** Fetch specified resource and return associated content. Resource should have benn previously accepted. */
  override def fetch(resource: String): Future[Content] = {
    Future.successful(new Content("", ""))
  }

  /** Accepts specified resource. */
  override def accepts(resource: String): Boolean = true

  override def withExecutionContext(ec: ExecutionContext): ResourceLoader = CustomECResourceLoader(ec)
}
