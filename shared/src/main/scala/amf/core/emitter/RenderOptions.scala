package amf.core.emitter

import amf.client.remod.amfcore.config.{RenderOptions => ImmutableRenderOptions}
import amf.client.remod.amfcore.config.{ShapeRenderOptions => ImmutableShapeRenderOptions}
import amf.client.render.{RenderOptions => ClientRenderOptions}
import amf.client.resolve.ClientErrorHandlerConverter._
import amf.core.errorhandling.{ErrorHandler, UnhandledErrorHandler}
import amf.core.metamodel.Field
import amf.plugins.document.graph.{
  EmbeddedForm,
  FlattenedForm,
  GraphSerialization,
  JsonLdSerialization,
  RdfSerialization
}

/**
  * Render options
  */
class RenderOptions {

  private var compactedEmission: Boolean     = true
  private var sources: Boolean               = false
  private var compactUris: Boolean           = false
  private var rawSourceMaps: Boolean         = false
  private var validating: Boolean            = false
  private var filterFields: Field => Boolean = (_: Field) => false
  private var amfJsonLdSerialization         = true
  private var useJsonLdEmitter               = false
  private var flattenedJsonLd                = false
  private var eh: ErrorHandler               = UnhandledErrorHandler
  private var prettyPrint                    = false
  private var emitNodeIds                    = false

  def withCompactedEmission: RenderOptions = {
    compactedEmission = true
    this
  }

  def withoutCompactedEmission: RenderOptions = {
    compactedEmission = false
    this
  }

  def withPrettyPrint: RenderOptions = {
    prettyPrint = true
    this
  }

  def withoutPrettyPrint: RenderOptions = {
    prettyPrint = true
    this
  }

  /** Include source maps when rendering to graph. */
  def withSourceMaps: RenderOptions = {
    sources = true
    this
  }

  /** Include source maps when rendering to graph. */
  def withoutSourceMaps: RenderOptions = {
    sources = false
    this
  }

  def withCompactUris: RenderOptions = {
    compactUris = true
    this
  }

  def withoutCompactUris: RenderOptions = {
    compactUris = false
    this
  }

  def withRawSourceMaps: RenderOptions = {
    rawSourceMaps = true
    this
  }

  def withoutRawSourceMaps: RenderOptions = {
    rawSourceMaps = false
    this
  }

  def withValidation: RenderOptions = {
    validating = true
    this
  }

  def withNodeIds: RenderOptions = {
    emitNodeIds = true
    this
  }

  def withoutNodeIds: RenderOptions = {
    emitNodeIds = false
    this
  }
  def withoutValidation: RenderOptions = {
    validating = false
    this
  }

  def withFilterFieldsFunc(f: Field => Boolean): RenderOptions = {
    filterFields = f
    this
  }

  def withoutAmfJsonLdSerialization: RenderOptions = {
    amfJsonLdSerialization = false
    this
  }

  def withAmfJsonLdSerialization: RenderOptions = {
    amfJsonLdSerialization = true
    this
  }

  def withErrorHandler(errorHandler: ErrorHandler): RenderOptions = {
    eh = errorHandler
    this
  }

  def withFlattenedJsonLd: RenderOptions = {
    flattenedJsonLd = true
    this
  }

  def withoutFlattenedJsonLd: RenderOptions = {
    flattenedJsonLd = false
    this
  }

  def isFlattenedJsonLd: Boolean = flattenedJsonLd

  def isWithCompactedEmission: Boolean   = compactedEmission
  def isCompactUris: Boolean             = compactUris
  def isWithSourceMaps: Boolean          = sources
  def isWithRawSourceMaps: Boolean       = rawSourceMaps
  def isAmfJsonLdSerilization: Boolean   = amfJsonLdSerialization
  def isValidation: Boolean              = validating
  def renderField(field: Field): Boolean = !filterFields(field)
  def errorHandler: ErrorHandler         = eh
  def isPrettyPrint: Boolean             = prettyPrint
  def isEmitNodeIds: Boolean             = emitNodeIds

  private[amf] def toGraphSerialization: GraphSerialization = {
    if (isAmfJsonLdSerilization) {
      if (isFlattenedJsonLd) {
        JsonLdSerialization(FlattenedForm)
      } else {
        JsonLdSerialization(EmbeddedForm)
      }
    } else {
      RdfSerialization()
    }
  }
}

object RenderOptions {
  def apply(): RenderOptions = new RenderOptions()

  def apply(client: ClientRenderOptions): RenderOptions = {
    val opts = new RenderOptions()
    opts.compactedEmission = client.isWithCompactedEmission
    opts.sources = client.isWithSourceMaps
    opts.amfJsonLdSerialization = client.isAmfJsonLdSerilization
    opts.compactUris = client.isWithCompactUris
    opts.prettyPrint = client.isPrettyPrint
    opts.flattenedJsonLd = client.isFlattenedJsonLd
    opts
  }

  def fromImmutable(opts: ImmutableRenderOptions, eh: ErrorHandler): RenderOptions = {
    val newOptions = new RenderOptions()
    newOptions.compactedEmission = opts.compactedEmission
    newOptions.sources = opts.sources
    newOptions.compactUris = opts.compactUris
    newOptions.rawSourceMaps = opts.rawSourceMaps
    newOptions.validating = opts.validating
    newOptions.filterFields = opts.filterFields
    newOptions.amfJsonLdSerialization = opts.amfJsonLdSerialization
    newOptions.useJsonLdEmitter = opts.useJsonLdEmitter
    newOptions.flattenedJsonLd = opts.flattenedJsonLd
    newOptions.eh = eh
    newOptions.prettyPrint = opts.prettyPrint
    newOptions.emitNodeIds = opts.emitNodeIds
    newOptions
  }

  def toImmutable(options: RenderOptions, shapeRenderOptions: ImmutableShapeRenderOptions): ImmutableRenderOptions =
    ImmutableRenderOptions(
        options.compactedEmission,
        options.sources,
        options.compactUris,
        options.rawSourceMaps,
        options.validating,
        options.filterFields,
        options.amfJsonLdSerialization,
        options.useJsonLdEmitter,
        options.flattenedJsonLd,
        options.prettyPrint,
        options.emitNodeIds,
        shapeRenderOptions
    )
}
