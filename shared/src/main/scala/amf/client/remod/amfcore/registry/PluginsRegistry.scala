package amf.client.remod.amfcore.registry

import amf.ProfileName
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.plugins.parse.{AMFParsePlugin, DomainParsingFallback, ExternalFragmentDomainFallback}
import amf.client.remod.amfcore.plugins.validate.AMFValidatePlugin
import amf.core.model.document.BaseUnit

case class PluginsRegistry private[amf] (parsePlugins: List[AMFParsePlugin],
                                         validatePlugins: List[AMFValidatePlugin],
                                         domainParsingFallback: DomainParsingFallback) {

  lazy val allPlugins: List[AMFPlugin[_]] = parsePlugins ++ validatePlugins

  def withPlugin(plugin: AMFPlugin[_]): PluginsRegistry = {
    plugin match {
      case p: AMFParsePlugin if !parsePlugins.exists(_.id == p.id) =>
        copy(parsePlugins = parsePlugins :+ p)
      case v: AMFValidatePlugin =>
        copy(validatePlugins = validatePlugins :+ v)
      case _ => this
    }
  }

  def withoutPlugin(plugin: AMFPlugin[_]): PluginsRegistry = {
    val pps = parsePlugins.filter(_ != plugin)
    val vps = validatePlugins.filter(_ != plugin)
    copy(parsePlugins = pps, validatePlugins = vps)
  }

  def withPlugins(plugins: List[AMFPlugin[_]]): PluginsRegistry = {
    plugins.foldLeft(this) { case (registry, plugin) => registry.withPlugin(plugin) }
  }
  //
  //  def getParsePluginFor(document:ParsedDocument, vendor: Vendor): AmfParsePlugin = pickPlugin(document, (p:AmfParsePlugin, d:YDocument) => p.apply(d, vendor))
  //
  //
  //  private def pickPlugin(document:ParsedDocument,selectorFn: (AmfParsePlugin,YDocument) => Boolean) = {
  //    ydocument(document).flatMap { ast =>
  //      reduceList(parsePlugins.filter(p => selectorFn(p, ast)))
  //    } getOrElse(defaultPlugin)
  //  }
  //
  //  private def reduceList(list:List[AmfParsePlugin]): Option[AmfParsePlugin] = {
  //    list match {
  //      case Nil         => None
  //      case head :: Nil => Some(head)
  //      case multiple    => Some(multiple.min)
  //    }
  //  }
  //
  //  def getParsePluginFor(document:ParsedDocument): AmfParsePlugin = pickPlugin(document, (p:AmfParsePlugin, d:YDocument) => p.applies(d))
  //  def getResolvePluginFor(bu: BaseUnit, vendor: Vendor): Option[AmfResolvePlugin]

  def getValidationsPlugin(bu: BaseUnit): Seq[AMFValidatePlugin] = ???

  def getValidationPlugin(bu: BaseUnit, profile: ProfileName): Option[AMFValidatePlugin] = ???
}

object PluginsRegistry {
  val empty: PluginsRegistry = PluginsRegistry(Nil, Nil, ExternalFragmentDomainFallback)
}
