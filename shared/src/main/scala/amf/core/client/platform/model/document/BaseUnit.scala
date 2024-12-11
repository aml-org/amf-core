package amf.core.client.platform.model.document

import amf.core.client.platform.model.domain.DomainElement
import amf.core.client.platform.model.{AmfObjectWrapper, StrField}
import amf.core.client.scala.model.document.{BaseUnit => InternalBaseUnit}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.convert.CoreClientConverters._
import amf.core.internal.remote.Spec
import amf.core.internal.unsafe.PlatformSecrets
import com.github.ghik.silencer.silent

import scala.scalajs.js.annotation.JSExportAll

/** Any parsable unit, backed by a source URI. */
@JSExportAll
trait BaseUnit extends AmfObjectWrapper with PlatformSecrets {

  override private[amf] val _internal: InternalBaseUnit

  def id: String = this._internal.id

  /** Returns the list document URIs referenced from the document that has been parsed to generate this model */
  def references(): ClientList[BaseUnit] = _internal.references.asClient

  /** Returns package */
  def pkg(): StrField = _internal.pkg

  /** Raw text  used to generated this unit */
  def raw: ClientOption[String] = _internal.raw.asClient

  /** Returns the file location for the document that has been parsed to generate this model */
  def location: String = _internal.location().getOrElse("")

  /** Returns element's usage comment */
  def usage: StrField = _internal.usage

  /** Returns the version. */
//  @deprecated("Use processingData.modelVersion for API Contract Base Units instead", "AMF 5.0.0 & AML 6.0.0")
  @silent
  def modelVersion: StrField = _internal.modelVersion

  /** Set references
    * @param references
    *   references to set
    * @return
    *   Previous [[BaseUnit]] with references set
    */
  def withReferences(references: ClientList[BaseUnit]): this.type = {
    _internal.withReferences(references.asInternal)
    this
  }

  /** Set package
    * @param pkg
    *   package to set
    * @return
    *   Previous [[BaseUnit]] with package set
    */
  def withPkg(pkg: String): this.type = {
    _internal.withPkg(pkg)
    this
  }

  /** Set id
    * @param id
    *   id to set
    * @return
    *   Previous [[BaseUnit]] with id set
    */
  def withId(id: String): this.type = {
    _internal.withId(id)
    this
  }

  /** Set raw text
    * @param raw
    *   raw text to set
    * @return
    *   Previous [[BaseUnit]] with raw text set
    */
  def withRaw(raw: String): this.type = {
    _internal.withRaw(raw)
    this
  }

  /** Set location
    * @param location
    *   location to set
    * @return
    *   Previous [[BaseUnit]] with location set
    */
  def withLocation(location: String): this.type = {
    _internal.withLocation(location)
    this
  }

  /** Set usage
    * @param usage
    *   usage to set
    * @return
    *   Previous [[BaseUnit]] with usage set
    */
  def withUsage(usage: String): this.type = {
    _internal.withUsage(usage)
    this
  }

  def findById(id: String): ClientOption[DomainElement] =
    _internal.findById(Namespace.defaultAliases.uri(id).iri()).asClient

  def findByType(typeId: String): ClientList[DomainElement] =
    _internal.findByType(Namespace.defaultAliases.expand(typeId).iri()).asClient

  def sourceSpec: ClientOption[Spec] = _internal.sourceSpec.asClient

  def cloneUnit(): BaseUnit = _internal.cloneUnit()

  def withReferenceAlias(alias: String, id: String, fullUrl: String, relativeUrl: String): BaseUnit = {
    _internal.withReferenceAlias(alias, id, fullUrl, relativeUrl)
    this
  }

  def processingData: BaseUnitProcessingData = _internal.processingData

  def withProcessingData(data: BaseUnitProcessingData): this.type = {
    _internal.withProcessingData(data)
    this
  }

  def sourceInformation: BaseUnitSourceInformation = _internal.sourceInformation
}
