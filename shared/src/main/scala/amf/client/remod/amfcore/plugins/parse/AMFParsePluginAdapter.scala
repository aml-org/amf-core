package amf.client.remod.amfcore.plugins.parse

import amf.client.plugins.AMFDocumentPlugin
import amf.client.remod.amfcore.plugins.PluginPriority
import amf.core.Root
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.parser.{ParserContext, ReferenceHandler}

private[amf] case class AMFParsePluginAdapter(plugin: AMFDocumentPlugin) extends AMFParsePlugin {

  override def parse(document: Root, ctx: ParserContext): BaseUnit = plugin.parse(document, ctx)

  override def validMediaTypesToReference: Seq[String] = plugin.validVendorsToReference

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = plugin.referenceHandler(eh)

  override def allowRecursiveReferences: Boolean = plugin.allowRecursiveReferences

  override val id: String = plugin.ID

  override def mediaTypes: Seq[String] = plugin.vendors

  override def applies(root: Root): Boolean = plugin.canParse(root)

  override def priority: PluginPriority = PluginPriority(plugin.priority)
}
