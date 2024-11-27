package amf.core.internal.remote.browser

import amf.core.client.common.remote.Content
import amf.core.client.platform.resource.BaseHttpResourceLoader
import amf.core.internal.remote.{NetworkError, UnexpectedStatusCode}
import org.scalajs.dom.ext.Ajax

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel("JsBrowserHttpResourceLoader")
@JSExportAll
case class JsBrowserHttpResourceLoader() extends BaseHttpResourceLoader {

  implicit def executionContext: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.queue

  override def fetch(resource: String): js.Promise[Content] = {
    Ajax
      .get(resource)
      .flatMap(xhr =>
        xhr.status match {
          case 200 => Future.successful(new Content(xhr.responseText, resource))
          case s   => Future.failed(UnexpectedStatusCode(resource, s))
        }
      )
      .recover { case e: Throwable =>
        throw NetworkError(e)
      }
      .toJSPromise
  }
}
