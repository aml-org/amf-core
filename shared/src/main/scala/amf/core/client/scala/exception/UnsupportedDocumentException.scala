package amf.core.client.scala.exception

class UnsupportedDocumentException(url: String, kind: String)
    extends Exception(s"Cannot find any registered $kind plugin for current document $url")

case class UnsupportedDomainForDocumentException(url: String) extends UnsupportedDocumentException(url, "domain")

case class UnsupportedSyntaxForDocumentException(url: String) extends UnsupportedDocumentException(url, "syntax")
