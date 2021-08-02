package amf.core.client.platform.model.document

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.platform.config.RenderOptions
import amf.core.client.platform.model.domain.DomainElement
import amf.core.client.platform.model.{AmfObjectWrapper, StrField}
import amf.core.internal.annotations.AliasDeclaration
import amf.core.client.scala.model.document.{BaseUnit => InternalBaseUnit}
import amf.core.internal.remote.Spec
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.client.scala.vocabulary.Namespace

import scala.scalajs.js.annotation.JSExportAll

/** Any parsable unit, backed by a source URI. */
@JSExportAll
trait BaseUnit extends AmfObjectWrapper with PlatformSecrets {

  override private[amf] val _internal: InternalBaseUnit

  def id: String = this._internal.id

  /** Returns the list document URIs referenced from the document that has been parsed to generate this model */
  def references(): ClientList[BaseUnit] = _internal.references.asClient

  /** Raw text  used to generated this unit */
  def raw: ClientOption[String] = _internal.raw.asClient

  /** Returns the file location for the document that has been parsed to generate this model */
  def location: String = _internal.location().getOrElse("")

  /** Returns element's usage comment */
  def usage: StrField = _internal.usage

  /** Returns the version. */
  def modelVersion: StrField = _internal.modelVersion

  def withReferences(references: ClientList[BaseUnit]): this.type = {
    _internal.withReferences(references.asInternal)
    this
  }

  def withId(id: String): this.type = {
    _internal.withId(id)
    this
  }

  def withRaw(raw: String): this.type = {
    _internal.withRaw(raw)
    this
  }

  def withLocation(location: String): this.type = {
    _internal.withLocation(location)
    this
  }

  def withUsage(usage: String): this.type = {
    _internal.withUsage(usage)
    this
  }

  def findById(id: String): ClientOption[DomainElement] =
    _internal.findById(Namespace.defaultAliases.uri(id).iri()).asClient

  def findByType(typeId: String): ClientList[DomainElement] =
    _internal.findByType(Namespace.defaultAliases.expand(typeId).iri()).asClient

  def sourceVendor: ClientOption[Spec] = _internal.sourceSpec.asClient

  def cloneUnit(): BaseUnit = _internal.cloneUnit()

  def withReferenceAlias(alias: String, fullUrl: String, relativeUrl: String): BaseUnit = {
    AliasDeclaration(_internal, alias, fullUrl, relativeUrl)
    this
  }
}
