package amf.client.remod.amfcore.plugins.render

import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.core.parser.{ParsedDocument, SyamlParsedDocument}
import amf.core.rdf.RdfModelDocument
import amf.plugins.syntax.SYamlSyntaxPlugin.{getFormat, platform, render}
import org.mulesoft.common.io.Output
import org.yaml.model.YDocument
import org.yaml.render.{JsonRender, JsonRenderOptions, YamlRender}

object SyamlSyntaxRenderPlugin extends AMFSyntaxRenderPlugin {

  override def emit[W: Output](mediaType: String, doc: ParsedDocument, writer: W): Option[W] = {
    doc match {
      case input: SyamlParsedDocument =>
        val ast = input.document
        render(mediaType, ast) { (format, ast) =>
          if (format == "yaml") YamlRender.render(writer, ast, expandReferences = false)
          else JsonRender.render(ast, writer, options = JsonRenderOptions().withoutNonAsciiEncode)
          Some(writer)
        }
      case input: RdfModelDocument if platform.rdfFramework.isDefined =>
        platform.rdfFramework.get.rdfModelToSyntaxWriter(mediaType, input, writer)
      case _ => None
    }
  }

  private def render[T](mediaType: String, ast: YDocument)(render: (String, YDocument) => T): T = {
    val format = getFormat(mediaType)
    render(format, ast)
  }

  private def getFormat(mediaType: String) = if (mediaType.contains("json")) "json" else "yaml"

  override def applies(element: ParsedDocument): Boolean = element.isInstanceOf[SyamlParsedDocument]

  override def mediaTypes: Seq[String] =
    Seq("application/yaml",
        "application/x-yaml",
        "text/yaml",
        "text/x-yaml",
        "application/json",
        "text/json",
        "text/vnd.yaml")

  override val id: String = "syaml-render"

  override def priority: PluginPriority = NormalPriority
}
