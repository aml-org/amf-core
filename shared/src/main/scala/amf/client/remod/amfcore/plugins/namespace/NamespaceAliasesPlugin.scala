package amf.client.remod.amfcore.plugins.namespace

import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.core.model.document.BaseUnit
import amf.core.vocabulary.NamespaceAliases

trait NamespaceAliasesPlugin extends AMFPlugin[BaseUnit] {
  def aliases(unit: BaseUnit): NamespaceAliases
}
