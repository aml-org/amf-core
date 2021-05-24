package amf.client.remod.amfcore.plugins.render

import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.core.parser.ParsedDocument
import org.mulesoft.common.io.Output
import org.yaml.model.YDocument

trait AMFSyntaxRenderPlugin extends AMFPlugin[ParsedDocument] {

  def emit[W: Output](mediaType: String, ast: ParsedDocument, writer: W): Option[W]

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  def mediaTypes: Seq[String]

}
