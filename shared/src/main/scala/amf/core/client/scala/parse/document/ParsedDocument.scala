package amf.core.client.scala.parse.document

import amf.core.internal.plugins.syntax.SourceCodeBlock
import org.yaml.model.YDocument

abstract class ParsedDocument {
  def comment: Option[String] = None
}

case class SyamlParsedDocument(document: YDocument, override val comment: Option[String] = None) extends ParsedDocument

case class StringParsedDocument(ast: SourceCodeBlock, override val comment: Option[String] = None)
    extends ParsedDocument
