package amf.core.internal.plugins.parse

import amf.core.client.common.{LowPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.exception.{UnsupportedDomainForDocumentException, UnsupportedParsedDocumentException}
import amf.core.client.scala.model.document.{BaseUnit, ExternalFragment}
import amf.core.client.scala.model.domain.ExternalDomainElement
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{
  ParserContext,
  ReferenceHandler,
  SimpleReferenceHandler,
  SyamlParsedDocument
}
import amf.core.internal.parser.Root
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.remote.{JSONRefs, Mimes, Spec, UnknownSpec}
import amf.core.internal.remote.Mimes.{`application/json`, `application/yaml`}
import amf.core.internal.utils.MediaTypeMatcher

trait DomainParsingFallback {

  def chooseFallback(root: Root, availablePlugins: Seq[AMFParsePlugin], isRoot: Boolean): AMFParsePlugin
}

case class ExternalFragmentDomainFallback(strict: Boolean = true) extends DomainParsingFallback {
  override def chooseFallback(root: Root, availablePlugins: Seq[AMFParsePlugin], isRoot: Boolean): AMFParsePlugin = {
    if (isRoot && strict) throw UnsupportedDomainForDocumentException(root.location)
    else {
      new AMFParsePlugin {
        override def spec: Spec = UnknownSpec("external-fragment")

        override def parse(document: Root, ctx: ParserContext): BaseUnit = {
          ExternalFragment()
            .withId(root.location)
            .withLocation(root.location)
            .withEncodes(
                ExternalDomainElement()
                  .withRaw(root.raw)
                  .withMediaType(root.mediatype))
        }

        /**
          * media types which specifies vendors that are parsed by this plugin.
          */
        override def mediaTypes: Seq[String] = Seq(root.mediatype)

        override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = SimpleReferenceHandler

        override def allowRecursiveReferences: Boolean = false

        override def applies(element: Root): Boolean = true

        override def priority: PluginPriority = LowPriority
      }

    }
  }
}
