package amf.plugins.render

import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.render.{AMFRenderPlugin, RenderInfo}
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.{BaseUnit, Document, Fragment, Module}
import amf.core.model.domain.DomainElement
import amf.core.remote.Vendor
import amf.plugins.common.Async20MediaTypes
import amf.plugins.document.apicontract.contexts.emitter.async.{Async20SpecEmitterContext, AsyncSpecEmitterContext}
import amf.plugins.document.apicontract.parser.spec.async.AsyncApi20DocumentEmitter
import amf.plugins.domain.apicontract.models.api.Api
import org.yaml.model.YDocument

object Async20RenderPlugin extends ApiRenderPlugin {

  override def vendor: Vendor = Vendor.ASYNC20

  override def priority: PluginPriority = NormalPriority

  override def defaultSyntax(): String = AMFRenderPlugin.APPLICATION_YAML

  override def mediaTypes: Seq[String] = Async20MediaTypes.mediaTypes

  override def applies(element: RenderInfo): Boolean = element.unit match {
    case document: Document => document.encodes.isInstanceOf[Api]
    case module: Module =>
      module.declares exists {
        case _: DomainElement => false
        case _                => false
      }
    case _: Fragment => false
    case _           => false
  }

  override protected def unparseAsYDocument(unit: BaseUnit,
                                            renderOptions: RenderOptions,
                                            errorHandler: AMFErrorHandler): Option[YDocument] = {
    unit match {
      case document: Document =>
        Some(new AsyncApi20DocumentEmitter(document)(specContext(renderOptions, errorHandler)).emitDocument())
      case _ => None
    }
  }

  private def specContext(options: RenderOptions, errorHandler: AMFErrorHandler): AsyncSpecEmitterContext =
    new Async20SpecEmitterContext(errorHandler)
}
