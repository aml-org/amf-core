package amf.core.internal.plugins.render

import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.{ParsedDocument, SyamlParsedDocument}
import amf.core.internal.plugins.syntax.ASTBuilder
import amf.core.internal.remote.Mimes.{`application/json`, `application/yaml`}
import org.yaml.builder.YDocumentBuilder
import org.yaml.model.YDocument

trait SYAMLBasedRenderPlugin extends AMFRenderPlugin {

  override def emit[T](unit: BaseUnit,
                       builder: ASTBuilder[T],
                       renderConfiguration: RenderConfiguration,
                       mediaType: String): Boolean = {
    builder match {
      case sb: YDocumentBuilder =>
        unparseAsYDocument(unit, renderConfiguration, renderConfiguration.errorHandler) exists { doc =>
          sb.document = doc
          true
        }
      case _ => false
    }
  }

  protected def unparseAsYDocument(unit: BaseUnit,
                                   renderConfig: RenderConfiguration,
                                   errorHandler: AMFErrorHandler): Option[YDocument]

  override def mediaTypes: Seq[String] = Seq(`application/json`, `application/yaml`)

  override def getDefaultBuilder: ASTBuilder[_] = new SYAMLASTBuilder

}

class SYAMLASTBuilder extends YDocumentBuilder with ASTBuilder[YDocument] {
  override def astResult: YDocument           = result.asInstanceOf[YDocument]
  override def parsedDocument: ParsedDocument = SyamlParsedDocument(astResult)
}
