package amf.core.client.platform.resource

import amf.core.client.common.remote.Content
import amf.core.client.platform.execution.BaseExecutionEnvironment
import amf.core.client.scala.lexer.CharArraySequence
import amf.core.internal.remote.FutureConverter._
import amf.core.internal.remote.{NetworkError, SocketTimeout, UnexpectedStatusCode}

import java.net.{HttpURLConnection, SocketTimeoutException, URI}
import java.util.concurrent.CompletableFuture
import scala.concurrent.{ExecutionContext, Future}

case class HttpResourceLoader(executionContext: ExecutionContext)
    extends BaseHttpResourceLoader
    with LoaderWithExecutionContext {

  implicit val exec: ExecutionContext = executionContext

  def this() = this(scala.concurrent.ExecutionContext.Implicits.global)
  def this(executionEnvironment: BaseExecutionEnvironment) = this(executionEnvironment.executionContext)

  override def withExecutionContext(newEc: ExecutionContext): ResourceLoader = HttpResourceLoader(newEc)

  override def fetch(resource: String): CompletableFuture[Content] = {
    val u          = new URI(resource).parseServerAuthority().toURL
    val connection = u.openConnection.asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    connection.setConnectTimeout(System.getProperty("amf.connection.connect.timeout", "5000").toInt)
    connection.setReadTimeout(System.getProperty("amf.connection.read.timeout", "60000").toInt)

    Future {
      try {
        connection.connect()
        connection.getResponseCode match {
          case 200 =>
            createContent(connection, resource)
          case s =>
            throw UnexpectedStatusCode(resource, s)
        }
      } catch {
        case ex: Exception             => throw NetworkError(ex)
        case e: SocketTimeoutException => throw SocketTimeout(e)
      }
    }.asJava
  }

  private def createContent(connection: HttpURLConnection, url: String): Content = {
    new Content(
      CharArraySequence(connection.getInputStream, connection.getContentLength, None).toString,
      url,
      Option(connection.getHeaderField("Content-Type"))
    )
  }
}
