package amf.client.remod.amfcore.registry

import amf.ProfileName
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.plugins.parse.{AMFParsePlugin, DomainParsingFallback, ExternalFragmentDomainFallback}
import amf.client.remod.amfcore.plugins.render.AMFRenderPlugin
import amf.client.remod.amfcore.plugins.validate.AMFValidatePlugin
import amf.core.model.document.BaseUnit

case class PluginsRegistry private[amf] (parsePlugins: List[AMFParsePlugin],
                                         validatePlugins: List[AMFValidatePlugin],
                                         renderPlugins: List[AMFRenderPlugin],
                                         domainParsingFallback: DomainParsingFallback) {

  lazy val allPlugins: List[AMFPlugin[_]] = parsePlugins ++ validatePlugins ++ renderPlugins

  def withPlugin(plugin: AMFPlugin[_]): PluginsRegistry = {
    plugin match {
      case p: AMFParsePlugin if !parsePlugins.exists(_.id == p.id) =>
        copy(parsePlugins = parsePlugins :+ p)
      case v: AMFValidatePlugin if !validatePlugins.exists(_.id == v.id) =>
        copy(validatePlugins = validatePlugins :+ v)
      case r: AMFRenderPlugin if !renderPlugins.exists(_.id == r.id) =>
        copy(renderPlugins = renderPlugins :+ r)
      case _ => this
    }
  }

  def withPlugins(plugins: Seq[AMFPlugin[_]]): PluginsRegistry = {
    plugins.foldLeft(this) { case (registry, plugin) => registry.withPlugin(plugin) }
  }

  def removePlugin(id: String): PluginsRegistry =
    copy(parsePlugins = parsePlugins.filterNot(_.id == id),
         validatePlugins = validatePlugins.filterNot(_.id == id),
         renderPlugins = renderPlugins.filterNot(_.id == id))

}

object PluginsRegistry {
  val empty: PluginsRegistry = PluginsRegistry(Nil, Nil, Nil, ExternalFragmentDomainFallback)
}
