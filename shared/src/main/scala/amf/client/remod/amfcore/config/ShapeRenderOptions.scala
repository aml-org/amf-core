package amf.client.remod.amfcore.config

import amf.client.render.{JSONSchemaVersion, JSONSchemaVersions}
import amf.core.errorhandling.{AMFErrorHandler, UnhandledErrorHandler}

/**
  * Immutable implementation of shape render options
  */
private[amf] case class ShapeRenderOptions(
    documentation: Boolean = true,
    compactedEmission: Boolean = false,
    emitWarningForUnsupportedValidationFacets: Boolean = false,
    schema: JSONSchemaVersion = JSONSchemaVersions.UNSPECIFIED,
) {

  /** Remove documentation info as examples, descriptions, display names, etc. */
  def withoutDocumentation: ShapeRenderOptions = copy(documentation = false)

  /** Render shape extracting common types to definitions */
  def withCompactedEmission: ShapeRenderOptions = copy(compactedEmission = true)

  def withEmitWarningForUnsupportedValidationFacets(value: Boolean): ShapeRenderOptions = copy(emitWarningForUnsupportedValidationFacets = value)

  def withSchemaVersion(version: JSONSchemaVersion): ShapeRenderOptions = copy(schema = version)

  def isWithDocumentation: Boolean = documentation
  def isWithCompactedEmission: Boolean = compactedEmission
  def shouldEmitWarningForUnsupportedValidationFacets: Boolean = emitWarningForUnsupportedValidationFacets
  def schemaVersion: JSONSchemaVersion = schema
}
