package amf.core.internal.plugins.syntax

import amf.core.client.scala.parse.document.ParsedDocument

trait ASTBuilder[T] {
  def astResult: T

  def parsedDocument: ParsedDocument
}
