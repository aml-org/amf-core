package amf.client.exported.config

import amf.client.model.document.BaseUnit
import amf.client.remod.amfcore.config.{
  FinishedParsingEvent => InternalFinishedParsingEvent,
  FinishedRenderingASTEvent => InternalFinishedRenderingASTEvent,
  FinishedRenderingSyntaxEvent => InternalFinishedRenderingSyntaxEvent,
  FinishedTransformationEvent => InternalFinishedTransformationEvent,
  FinishedTransformationStepEvent => InternalFinishedTransformationStepEvent,
  FinishedValidationEvent => InternalFinishedValidationEvent,
  FinishedValidationPluginEvent => InternalFinishedValidationPluginEvent,
  ParsedModelEvent => InternalParsedModelEvent,
  ParsedSyntaxEvent => InternalParsedSyntaxEvent,
  StartingContentParsingEvent => InternalStartingContentParsingEvent,
  StartingParsingEvent => InternalStartingParsingEvent,
  StartingRenderingEvent => InternalStartingRenderingEvent,
  StartingTransformationEvent => InternalStartingTransformationEvent,
  StartingValidationEvent => InternalStartingValidationEvent
}
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.convert.CoreClientConverters._
import amf.client.convert.TransformationPipelineConverter._
import amf.client.remod.amfcore.config.{AMFEvent => InternalAMFEvent}
import amf.client.exported.transform.{TransformationPipeline, TransformationStep}
import amf.client.remote.Content
import amf.client.validate.AMFValidationReport

/**
  * Defines an event listener linked to a specific {@link AMFEvent}
  */
@JSExportAll
trait AMFEventListener {
  def notifyEvent(event: AMFEvent)
}

@JSExportAll
sealed trait AMFEvent {
  val name: String
}

@JSExportTopLevel("EventNames")
@JSExportAll
object AMFEventNames {
  val StartingParsing            = "StartingParsing"
  val StartingContentParsing     = "StartingContentParsing"
  val ParsedSyntax               = "ParsedSyntax"
  val ParsedModel                = "ParsedModel"
  val FinishedParsing            = "FinishedParsing"
  val StartingTransformation     = "StartingTransformation"
  val FinishedTransformationStep = "FinishedTransformationStep"
  val FinishedTransformation     = "FinishedTransformation"
  val StartingValidation         = "StartingValidation"
  val FinishedValidationPlugin   = "FinishedValidationPlugin"
  val FinishedValidation         = "FinishedValidation"
  val StartingRendering          = "StartingRendering"
  val FinishedRenderingAST       = "FinishedRenderingAST"
  val FinishedRenderingSyntax    = "FinishedRenderingSyntax"
}

object AMFEventConverter {
  def asClient(e: InternalAMFEvent): AMFEvent = e match {
    case e: InternalStartingParsingEvent            => new StartingParsingEvent(e)
    case e: InternalStartingContentParsingEvent     => new StartingContentParsingEvent(e)
    case e: InternalParsedSyntaxEvent               => new ParsedSyntaxEvent(e)
    case e: InternalParsedModelEvent                => new ParsedModelEvent(e)
    case e: InternalFinishedParsingEvent            => new FinishedParsingEvent(e)
    case e: InternalStartingTransformationEvent     => new StartingTransformationEvent(e)
    case e: InternalFinishedTransformationStepEvent => new FinishedTransformationStepEvent(e)
    case e: InternalFinishedTransformationEvent     => new FinishedTransformationEvent(e)
    case e: InternalStartingValidationEvent         => new StartingValidationEvent(e)
    case e: InternalFinishedValidationPluginEvent   => new FinishedValidationPluginEvent(e)
    case e: InternalFinishedValidationEvent         => new FinishedValidationEvent(e)
    case e: InternalStartingRenderingEvent          => new StartingRenderingEvent(e)
    case e: InternalFinishedRenderingASTEvent       => new FinishedRenderingASTEvent(e)
    case e: InternalFinishedRenderingSyntaxEvent    => new FinishedRenderingSyntaxEvent(e)
  }
}

abstract class ClientEvent(private val _internal: InternalAMFEvent) extends AMFEvent {
  override val name: String = _internal.name
}

// Parsing Events

/**
  * every client invocation to the parsing logic
  */
@JSExportAll
class StartingParsingEvent(private val _internal: InternalStartingParsingEvent) extends ClientEvent(_internal) {
  def url: String                     = _internal.url
  def mediaType: ClientOption[String] = _internal.mediaType.asClient
}

/**
  * called before parsing syntax of certain content.
  */
@JSExportAll
class StartingContentParsingEvent(private val _internal: InternalStartingContentParsingEvent)
    extends ClientEvent(_internal) {
  def url: String      = _internal.url
  def content: Content = _internal.content
}

/**
  * every successful syntax AST being parsed for any document
  */
@JSExportAll
class ParsedSyntaxEvent(private val _internal: InternalParsedSyntaxEvent) extends ClientEvent(_internal) {
  def url: String      = _internal.url
  def content: Content = _internal.content
}

/**
  * every successful domain model being parsed for any document
  */
@JSExportAll
class ParsedModelEvent(private val _internal: InternalParsedModelEvent) extends ClientEvent(_internal) {
  def url: String    = _internal.url
  def unit: BaseUnit = _internal.unit
}

/**
  * every successful parser invocation containing the top level domain unit being parsed
  */
@JSExportAll
class FinishedParsingEvent(private val _internal: InternalFinishedParsingEvent) extends ClientEvent(_internal) {
  def url: String    = _internal.url
  def unit: BaseUnit = _internal.unit
}

// Resolution Events

@JSExportAll
class StartingTransformationEvent(private val _internal: InternalStartingTransformationEvent)
    extends ClientEvent(_internal) {
  def pipeline: TransformationPipeline = _internal.pipeline
}

@JSExportAll
class FinishedTransformationStepEvent(private val _internal: InternalFinishedTransformationStepEvent)
    extends ClientEvent(_internal) {
  def step: TransformationStep = _internal.step
  def index: Int               = _internal.index
}

@JSExportAll
class FinishedTransformationEvent(private val _internal: InternalFinishedTransformationEvent)
    extends ClientEvent(_internal) {
  def unit: BaseUnit = _internal.unit
}

// Validation Events

@JSExportAll
class StartingValidationEvent(private val _internal: InternalStartingValidationEvent) extends ClientEvent(_internal) {
  def totalPlugins: Int = _internal.totalPlugins
}

@JSExportAll
class FinishedValidationPluginEvent(private val _internal: InternalFinishedValidationPluginEvent)
    extends ClientEvent(_internal) {
  def result: AMFValidationReport = _internal.result
}

@JSExportAll
class FinishedValidationEvent(private val _internal: InternalFinishedValidationEvent) extends ClientEvent(_internal) {
  def result: AMFValidationReport = _internal.result
}

// Rendering Events

@JSExportAll
class StartingRenderingEvent(private val _internal: InternalStartingRenderingEvent) extends ClientEvent(_internal) {
  def unit: BaseUnit    = _internal.unit
  def mediaType: String = _internal.mediaType
}

@JSExportAll
class FinishedRenderingASTEvent(private val _internal: InternalFinishedRenderingASTEvent)
    extends ClientEvent(_internal) {
  def unit: BaseUnit = _internal.unit
}

@JSExportAll
class FinishedRenderingSyntaxEvent(private val _internal: InternalFinishedRenderingSyntaxEvent)
    extends ClientEvent(_internal) {
  def unit: BaseUnit = _internal.unit
}
