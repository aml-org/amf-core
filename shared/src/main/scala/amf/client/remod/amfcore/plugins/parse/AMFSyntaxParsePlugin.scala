package amf.client.remod.amfcore.plugins.parse

import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.core.parser.{ParsedDocument, ParserContext}

trait AMFSyntaxParsePlugin extends AMFPlugin[CharSequence] {

  def parse(text: CharSequence, mediaType: String, ctx: ParserContext): ParsedDocument

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  def mediaTypes: Seq[String]

}
