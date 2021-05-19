package amf.client.remod.amfcore.plugins.parse

import amf.client.remod.ParseConfiguration
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.core.model.document.BaseUnit
import amf.core.parser.ParsedDocument

trait AMFSyntaxPlugin extends AMFPlugin[CharSequence] {

  def parse(text: CharSequence, mediaType: String, config: ParseConfiguration): ParsedDocument

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  def mediaTypes: Seq[String]

}
