package amf.core.internal.remote

import amf.core.client.common.AmfExceptionCode

case class FileNotFound(cause: Throwable)
    extends FileLoaderException(AmfExceptionCode.FileNotFound, "File Not Found: " + cause.getMessage, cause)

case class SocketTimeout(cause: Throwable)
    extends FileLoaderException(AmfExceptionCode.SocketTimeout, "Socket Timeout: " + cause.getMessage, cause)

case class NetworkError(cause: Throwable)
    extends FileLoaderException(AmfExceptionCode.NetworkError, "Network Error: " + cause.getMessage, cause)

case class UnexpectedStatusCode(resource: String, statusCode: Int)
    extends AmfException(
      AmfExceptionCode.UnexpectedStatusCode,
      s"Unexpected status code '$statusCode' for resource '$resource'"
    )

class UnsupportedUrlScheme(url: String)
    extends AmfException(AmfExceptionCode.UnsupportedUrlScheme, "Unsupported Url scheme: " + url)

class PathResolutionError(message: String)
    extends AmfException(AmfExceptionCode.PathResolutionError, "Error resolving path: " + message)

abstract class FileLoaderException(code: String, msj: String, e: Throwable) extends AmfException(code, msj, e)

abstract class AmfException(val code: String, val message: String, e: Throwable = new Throwable())
    extends Exception(message, e)
