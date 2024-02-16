package amf.core.client.scala.exception

abstract class AmfUnhandledException(val error: String) extends RuntimeException(error)
