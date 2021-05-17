package amf.client.remod.amfcore.plugins.parse

import amf.core.Root
import amf.core.exception.UnsupportedVendorException
import amf.core.model.document.{BaseUnit, ExternalFragment}
import amf.core.model.domain.ExternalDomainElement

trait DomainParsingFallback {

  def chooseFallback(root: Root, mediaType: Option[String], availablePlugins: Seq[AMFParsePlugin]): BaseUnit
}

object ExternalFragmentDomainFallback extends DomainParsingFallback {
  override def chooseFallback(root: Root, mediaType: Option[String], availablePlugins: Seq[AMFParsePlugin]): BaseUnit = {
    mediaType match {
      case Some(definedVendor) =>
        throw new UnsupportedVendorException(definedVendor)
      case None =>
        ExternalFragment()
          .withId(root.location)
          .withLocation(root.location)
          .withEncodes(
              ExternalDomainElement()
                .withRaw(root.raw)
                .withMediaType(root.mediatype))
    }

  }
}
