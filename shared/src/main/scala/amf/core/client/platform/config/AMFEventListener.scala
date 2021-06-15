package amf.core.client.platform.config

import amf.core.client.platform.model.document.BaseUnit

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.core.internal.convert.CoreClientConverters._
import amf.core.internal.convert.TransformationPipelineConverter._
import amf.core.client.platform.transform.{TransformationPipeline, TransformationStep}
import amf.core.client.common.remote.Content
import amf.core.client.scala.config
import amf.core.client.platform.validation.AMFValidationReport

/**
  * Defines an event listener linked to a specific [[AMFEvent]]
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
  def asClient(e: config.AMFEvent): AMFEvent = e match {
    case e: config.StartingParsingEvent            => new StartingParsingEvent(e)
    case e: config.StartingContentParsingEvent     => new StartingContentParsingEvent(e)
    case e: config.ParsedSyntaxEvent               => new ParsedSyntaxEvent(e)
    case e: config.ParsedModelEvent                => new ParsedModelEvent(e)
    case e: config.FinishedParsingEvent            => new FinishedParsingEvent(e)
    case e: config.StartingTransformationEvent     => new StartingTransformationEvent(e)
    case e: config.FinishedTransformationStepEvent => new FinishedTransformationStepEvent(e)
    case e: config.FinishedTransformationEvent     => new FinishedTransformationEvent(e)
    case e: config.StartingValidationEvent         => new StartingValidationEvent(e)
    case e: config.FinishedValidationPluginEvent   => new FinishedValidationPluginEvent(e)
    case e: config.FinishedValidationEvent         => new FinishedValidationEvent(e)
    case e: config.StartingRenderingEvent          => new StartingRenderingEvent(e)
    case e: config.FinishedRenderingASTEvent       => new FinishedRenderingASTEvent(e)
    case e: config.FinishedRenderingSyntaxEvent    => new FinishedRenderingSyntaxEvent(e)
  }
}

abstract class ClientEvent(private val _internal: config.AMFEvent) extends AMFEvent {
  override val name: String = _internal.name
}

// Parsing Events

/**
  * every client invocation to the parsing logic
  */
@JSExportAll
class StartingParsingEvent(private val _internal: config.StartingParsingEvent) extends ClientEvent(_internal) {
  def url: String                     = _internal.url
  def mediaType: ClientOption[String] = _internal.mediaType.asClient
}

/**
  * called before parsing syntax of certain content.
  */
@JSExportAll
class StartingContentParsingEvent(private val _internal: config.StartingContentParsingEvent)
    extends ClientEvent(_internal) {
  def url: String      = _internal.url
  def content: Content = _internal.content
}

/**
  * every successful syntax AST being parsed for any document
  */
@JSExportAll
class ParsedSyntaxEvent(private val _internal: config.ParsedSyntaxEvent) extends ClientEvent(_internal) {
  def url: String      = _internal.url
  def content: Content = _internal.content
}

/**
  * every successful domain model being parsed for any document
  */
@JSExportAll
class ParsedModelEvent(private val _internal: config.ParsedModelEvent) extends ClientEvent(_internal) {
  def url: String    = _internal.url
  def unit: BaseUnit = _internal.unit
}

/**
  * every successful parser invocation containing the top level domain unit being parsed
  */
@JSExportAll
class FinishedParsingEvent(private val _internal: config.FinishedParsingEvent) extends ClientEvent(_internal) {
  def url: String    = _internal.url
  def unit: BaseUnit = _internal.unit
}

// Resolution Events

@JSExportAll
class StartingTransformationEvent(private val _internal: config.StartingTransformationEvent)
    extends ClientEvent(_internal) {
  def pipeline: TransformationPipeline = _internal.pipeline
}

@JSExportAll
class FinishedTransformationStepEvent(private val _internal: config.FinishedTransformationStepEvent)
    extends ClientEvent(_internal) {
  def step: TransformationStep = _internal.step
  def index: Int               = _internal.index
}

@JSExportAll
class FinishedTransformationEvent(private val _internal: config.FinishedTransformationEvent)
    extends ClientEvent(_internal) {
  def unit: BaseUnit = _internal.unit
}

// Validation Events

@JSExportAll
class StartingValidationEvent(private val _internal: config.StartingValidationEvent) extends ClientEvent(_internal) {
  def totalPlugins: Int = _internal.totalPlugins
}

@JSExportAll
class FinishedValidationPluginEvent(private val _internal: config.FinishedValidationPluginEvent)
    extends ClientEvent(_internal) {
  def result: AMFValidationReport = _internal.result
}

@JSExportAll
class FinishedValidationEvent(private val _internal: config.FinishedValidationEvent) extends ClientEvent(_internal) {
  def result: AMFValidationReport = _internal.result
}

// Rendering Events

@JSExportAll
class StartingRenderingEvent(private val _internal: config.StartingRenderingEvent) extends ClientEvent(_internal) {
  def unit: BaseUnit    = _internal.unit
  def mediaType: String = _internal.mediaType
}

@JSExportAll
class FinishedRenderingASTEvent(private val _internal: config.FinishedRenderingASTEvent)
    extends ClientEvent(_internal) {
  def unit: BaseUnit = _internal.unit
}

@JSExportAll
class FinishedRenderingSyntaxEvent(private val _internal: config.FinishedRenderingSyntaxEvent)
    extends ClientEvent(_internal) {
  def unit: BaseUnit = _internal.unit
}