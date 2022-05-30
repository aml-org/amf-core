package amf.core.client.scala.exception

/** Exception describing cyclic references
  */
class CyclicReferenceException(val history: List[String])
    extends RuntimeException(s"Cyclic found following references ${history.mkString(" -> ")}")
