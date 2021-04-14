package amf.client.remod.amfcore.plugins.parse

import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.core.Root
import amf.core.client.ParsingOptions
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.parser.{ParserContext, ReferenceHandler}

private[amf] trait AMFParsePlugin extends AMFPlugin[ParsingInfo] {

//  def parse(document:Root, ctx:ParserContext): BaseUnit // change parser for AMF context
  def parse(document: Root, ctx: ParserContext, options: ParsingOptions): BaseUnit

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  def mediaTypes: Seq[String]

  /**
    * media types which specifies vendors that may be referenced.
    */
  def validMediaTypesToReference: Seq[String]

  def referenceHandler(eh: ErrorHandler): ReferenceHandler

  // move to some vendor/dialect configuration?
  def allowRecursiveReferences: Boolean
}
