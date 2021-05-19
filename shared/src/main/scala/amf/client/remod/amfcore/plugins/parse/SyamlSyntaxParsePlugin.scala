package amf.client.remod.amfcore.plugins.parse

import amf.client.remod.ParseConfiguration
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.core.parser.{JsonParserFactory, ParsedDocument, SyamlParsedDocument}
import amf.plugins.syntax.SYamlSyntaxPlugin.platform
import org.yaml.model.{YComment, YDocument, YMap, YNode}
import org.yaml.parser.YamlParser

object SyamlSyntaxParsePlugin extends AMFSyntaxPlugin {

  private def getFormat(mediaType: String) = if (mediaType.contains("json")) "json" else "yaml"

  override def parse(text: CharSequence, mediaType: String, config: ParseConfiguration): ParsedDocument = {
    if (text.length() == 0) SyamlParsedDocument(YDocument(YNode.Null))
    else if ((mediaType == "application/ld+json" || mediaType == "application/json") && !config.parsingOptions.isAmfJsonLdSerialization && platform.rdfFramework.isDefined) {
      platform.rdfFramework.get.syntaxToRdfModel(mediaType, text)
    } else {
      val parser = getFormat(mediaType) match {
        case "json" => JsonParserFactory.fromCharsWithSource(text, config.parserContext.rootContextDocument)(config.eh)
        case _      => YamlParser(text, config.parserContext.rootContextDocument)(config.eh).withIncludeTag("!include")
      }
      val document1 = parser.document()
      val (document, comment) = document1 match {
        case d if d.isNull =>
          (YDocument(Array(YNode(YMap.empty)), config.parserContext.rootContextDocument), d.children collectFirst {
            case c: YComment => c.metaText
          })
        case d =>
          (d, d.children collectFirst { case c: YComment => c.metaText })
      }
      SyamlParsedDocument(document, comment)
    }
  }

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] =
    Seq("application/yaml",
        "application/x-yaml",
        "text/yaml",
        "text/x-yaml",
        "application/json",
        "text/json",
        "text/vnd.yaml")

  override val id: String = "syaml"

  override def applies(element: CharSequence): Boolean = element.length() > 0

  override def priority: PluginPriority = NormalPriority
}
