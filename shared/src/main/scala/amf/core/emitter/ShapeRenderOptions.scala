package amf.core.emitter

import amf.client.render.{JSONSchemaVersion, JSONSchemaVersions, ShapeRenderOptions => ClientShapeRenderOptions}
import amf.client.resolve.ClientErrorHandlerConverter._
import amf.core.errorhandling.{AMFErrorHandler, UnhandledErrorHandler}
import amf.client.remod.amfcore.config.{ShapeRenderOptions => ImmutableShapeRenderOptions}

/**
  * JSON Schema options
  */
class ShapeRenderOptions {

  private var documentation: Boolean                             = true
  private var compactedEmission: Boolean                         = false
  private var emitWarningForUnsupportedValidationFacets: Boolean = false
  private var schema: JSONSchemaVersion                          = JSONSchemaVersions.UNSPECIFIED

  private var eh: AMFErrorHandler = UnhandledErrorHandler

  /** Remove documentation info as examples, descriptions, display names, etc. */
  def withoutDocumentation: ShapeRenderOptions = {
    documentation = false
    this
  }

  /** Render shape extracting common types to definitions */
  def withCompactedEmission: ShapeRenderOptions = {
    compactedEmission = true
    this
  }

  def withEmitWarningForUnsupportedValidationFacets(value: Boolean): ShapeRenderOptions = {
    emitWarningForUnsupportedValidationFacets = value
    this
  }

  def withErrorHandler(errorHandler: AMFErrorHandler): ShapeRenderOptions = {
    eh = errorHandler
    this
  }

  def withSchemaVersion(version: JSONSchemaVersion): ShapeRenderOptions = {
    schema = version
    this
  }

  def isWithDocumentation: Boolean                             = documentation
  def isWithCompactedEmission: Boolean                         = compactedEmission
  def shouldEmitWarningForUnsupportedValidationFacets: Boolean = emitWarningForUnsupportedValidationFacets
  def errorHandler: AMFErrorHandler                            = eh
  def schemaVersion: JSONSchemaVersion                         = schema
}

object ShapeRenderOptions {
  def apply(): ShapeRenderOptions = new ShapeRenderOptions()

  def apply(client: ClientShapeRenderOptions): ShapeRenderOptions = {
    val opts = new ShapeRenderOptions()
    opts.documentation = client.isWithDocumentation
    opts.compactedEmission = client.isWithCompactedEmission
    opts.eh = ErrorHandlerConverter.asInternal(client.errorHandler)
    opts.withSchemaVersion(client.schemaVersion)
    opts
  }

  def fromImmutable(opts: ImmutableShapeRenderOptions, eh: AMFErrorHandler): ShapeRenderOptions = {
    val newOptions = new ShapeRenderOptions()
    newOptions.documentation = opts.documentation
    newOptions.compactedEmission = opts.compactedEmission
    newOptions.emitWarningForUnsupportedValidationFacets = opts.emitWarningForUnsupportedValidationFacets
    newOptions.schema = opts.schema
    newOptions.eh = eh
    newOptions
  }

  def toImmutable(options: ShapeRenderOptions): ImmutableShapeRenderOptions =
    ImmutableShapeRenderOptions(
        options.documentation,
        options.compactedEmission,
        options.emitWarningForUnsupportedValidationFacets,
        options.schema
    )
}
