package amf.client.exported.config

import amf.client.remod.amfcore.config.{RenderOptions => InternalRenderOptions}
import amf.client.convert.CoreClientConverters._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("RenderOptions")
case class RenderOptions(private[amf] val _internal: InternalRenderOptions) {

  def this() = this(InternalRenderOptions())

  def withCompactedEmission: RenderOptions = _internal.withCompactedEmission

  /** Emit not compacted graph. */
  def withoutCompactedEmission: RenderOptions = _internal.withoutCompactedEmission

  /** Pretty print the graph. */
  def withPrettyPrint: RenderOptions = _internal.withPrettyPrint

  /** Not Pretty print the graph. */
  def withoutPrettyPrint: RenderOptions = _internal.withoutPrettyPrint

  /** Include source maps when rendering to graph. */
  def withSourceMaps: RenderOptions = _internal.withSourceMaps

  /** Include source maps when rendering to graph. */
  def withoutSourceMaps: RenderOptions = _internal.withoutSourceMaps

  def withCompactUris: RenderOptions = _internal.withCompactUris

  def withoutCompactUris: RenderOptions = _internal.withoutCompactUris

  def withFlattenedJsonLd: RenderOptions = _internal.withFlattenedJsonLd

  def withoutFlattenedJsonLd: RenderOptions = _internal.withoutFlattenedJsonLd

  /**
    * Emit specific AMF JSON-LD serialization
    *
    * @return
    */
  def withoutAmfJsonLdSerialization: RenderOptions = _internal.withoutAmfJsonLdSerialization

  /**
    * Emit regular JSON-LD serialization
    *
    * @return
    */
  def withAmfJsonLdSerialization: RenderOptions = _internal.withAmfJsonLdSerialization

  def withNodeIds: RenderOptions = _internal.withNodeIds

  def withShapeRenderOptions(s: ShapeRenderOptions): RenderOptions = _internal.withShapeRenderOptions(s)

  def isWithCompactedEmission: Boolean       = _internal.compactedEmission
  def isWithCompactUris: Boolean             = _internal.compactUris
  def isWithSourceMaps: Boolean              = _internal.sources
  def isAmfJsonLdSerilization: Boolean       = _internal.amfJsonLdSerialization
  def isPrettyPrint: Boolean                 = _internal.prettyPrint
  def isEmitNodeIds: Boolean                 = _internal.emitNodeIds
  def isFlattenedJsonLd: Boolean             = _internal.isFlattenedJsonLd
  def shapeRenderOptions: ShapeRenderOptions = _internal.shapeRenderOptions
}
