package amf.client.exported.config

import amf.client.render.JSONSchemaVersion
import amf.client.convert.CoreClientConverters._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.remod.amfcore.config.{ShapeRenderOptions => InternalShapeRenderOptions}

@JSExportAll
@JSExportTopLevel("ShapeRenderOptions")
case class ShapeRenderOptions(private[amf] val _internal: InternalShapeRenderOptions) {

  def this() = this(InternalShapeRenderOptions())

  def isWithDocumentation: Boolean     = _internal.isWithDocumentation
  def isWithCompactedEmission: Boolean = _internal.isWithCompactedEmission
  def schemaVersion: JSONSchemaVersion = _internal.schemaVersion

  /** Remove documentation info as examples, descriptions, display names, etc. */
  def withoutDocumentation: ShapeRenderOptions = _internal.withoutDocumentation

  /** Render shape extracting common types to definitions. */
  def withCompactedEmission: ShapeRenderOptions = _internal.withCompactedEmission

  def withSchemaVersion(version: JSONSchemaVersion): ShapeRenderOptions = _internal.withSchemaVersion(version)
}
