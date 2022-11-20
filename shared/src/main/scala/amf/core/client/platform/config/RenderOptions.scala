package amf.core.client.platform.config

import amf.core.client.common.render.JSONSchemaVersion
import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.scala.config.{RenderOptions => InternalRenderOptions}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class RenderOptions(private[amf] val _internal: InternalRenderOptions) {

  @JSExportTopLevel("RenderOptions")
  def this() = this(InternalRenderOptions())

  /** Pretty print the graph. */
  def withPrettyPrint(): RenderOptions = _internal.withPrettyPrint

  /** Not Pretty print the graph. */
  def withoutPrettyPrint(): RenderOptions = _internal.withoutPrettyPrint

  /** Include source maps when rendering to graph. */
  def withSourceMaps(): RenderOptions = _internal.withSourceMaps

  /** Exclude  source maps when rendering to graph. */
  def withoutSourceMaps(): RenderOptions = _internal.withoutSourceMaps

  /** Include source information node when rendering to graph. */
  def withSourceInformation(): RenderOptions = _internal.withSourceInformation

  /** Exclude source information node when rendering to graph. */
  def withoutSourceInformation(): RenderOptions = _internal.withSourceInformation

  /** Emits JSON-LD with compact IRIs when rendering to graph. */
  def withCompactUris(): RenderOptions = _internal.withCompactUris

  /** Don't emit JSON-LD with compact IRIs when rendering to graph. */
  def withoutCompactUris(): RenderOptions = _internal.withoutCompactUris

  /** Emit specific AMF JSON-LD serialization */
  def withoutAmfJsonLdSerialization(): RenderOptions = _internal.withoutAmfJsonLdSerialization

  /** Emit flattened JSON-LD serialization */
  def withAmfJsonLdSerialization(): RenderOptions = _internal.withAmfJsonLdSerialization

  /** Include Node IDs in rendering */
  def withNodeIds(): RenderOptions = _internal.withNodeIds

  /** Include documentation info in rendering such as examples, descriptions, display names, etc. (only supported for
    * json schema rendering)
    */
  def withDocumentation(): RenderOptions = _internal.withDocumentation

  /** Remove documentation info as examples, descriptions, display names, etc. (only supported for json schema
    * rendering)
    */
  def withoutDocumentation(): RenderOptions = _internal.withoutDocumentation

  /** Render shapes extracting common types to definitions (feature is enable by default for OAS and json schema) */
  def withCompactedEmission(): RenderOptions = _internal.withCompactedEmission

  /** Do not extract common types to definitions */
  def withoutCompactedEmission(): RenderOptions = _internal.withoutCompactedEmission

  /** Render shapes with specific json schema version (supported for json schema rendering) */
  def withSchemaVersion(version: JSONSchemaVersion): RenderOptions = _internal.withSchemaVersion(version)

  /** Emit raw field with original external content at graph */
  def withRawFieldEmission(): RenderOptions = _internal.withRawFieldEmission

  /** Include a reduced version of source maps when rendering to graph. */
  def withGovernanceMode: RenderOptions = _internal.withGovernanceMode

  /** Always render `type` facade on types even if the type is already clear by a unique facade. */
  def withoutImplicitRamlTypes(): RenderOptions = _internal.withoutImplicitRamlTypes

  def isWithDocumentation: Boolean     = _internal.isWithDocumentation
  def isWithCompactedEmission: Boolean = _internal.isWithCompactedEmission
  def schemaVersion: JSONSchemaVersion = _internal.schemaVersion

  def isWithCompactUris: Boolean        = _internal.compactUris
  def isWithSourceMaps: Boolean         = _internal.sources
  def isWithSourceInformation: Boolean  = _internal.sourceInformation
  def isAmfJsonLdSerialization: Boolean = _internal.amfJsonLdSerialization
  def isPrettyPrint: Boolean            = _internal.prettyPrint
  def isEmitNodeIds: Boolean            = _internal.emitNodeIds
  def isRawFieldEmission: Boolean       = _internal.rawFieldEmission
  def isGovernanceMode: Boolean         = _internal.governanceMode
}
