package amf.core.client.common

import amf.core.internal.remote.AmfException

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel("ExceptionUtil")
@JSExportAll
object ExceptionUtil {

  /** Checks if some AmfObject is of an specific type
    *
    * @param exception
    *   AmfObject to validate the type
    * @param exceptionCode
    *   IRI of the type wanted to validate. It could be one of the IRIs defined in [[AmfExceptionCode]] or other
    * @return
    *   a boolean value indicating if the AmfElement is of that type (true if it is, false if not)
    */
  def isExceptionType(exception: AmfException, exceptionCode: String): Boolean = {
    exception.code == exceptionCode
  }

}

@JSExportTopLevel("AmfExceptionCode")
@JSExportAll
object AmfExceptionCode {

  val ResourceNotFound: String     = "resource-not-found"
  val PathResolutionError: String  = "path-resolution-error"
  val UnsupportedUrlScheme: String = "unsupported-url-scheme"
  val UnexpectedStatusCode: String = "unexpected-status-code"
  val NetworkError: String         = "network-error"
  val SocketTimeout: String        = "socket-timeout"
  val FileNotFound: String         = "file-not-found"

}

