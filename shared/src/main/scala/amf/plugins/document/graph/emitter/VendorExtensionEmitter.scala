package amf.plugins.document.graph.emitter

import amf.core.emitter.EntryEmitter
import amf.core.model.domain.DomainElement
import amf.core.registries.AMFPluginsRegistry

object VendorExtensionEmitter {
  def emit(element: DomainElement, keyDecorator: String => String): Seq[EntryEmitter] = {
    if (element.extendedFields.nonEmpty) {
      val emitters = element.extendedFields.fields().flatMap { fieldEntry =>
        AMFPluginsRegistry.documentPlugins.find(_.canEmitExtension(fieldEntry.field)).map { plugin =>
          plugin.emitVendorExtensions(element, fieldEntry.field, keyDecorator)
        } getOrElse(Nil)
      }
      emitters.toSeq
    } else Nil
  }
}
