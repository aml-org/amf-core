package amf.client.remod.amfcore.config

import amf.client.remod.amfcore.config.AMFEventNames._
import amf.client.remod.amfcore.plugins.render.AMFRenderPlugin
import amf.client.remod.amfcore.plugins.validate.{AMFValidatePlugin, ValidationResult}
import amf.client.remote.Content
import amf.core.model.document.BaseUnit
import amf.core.parser.ParsedDocument
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.TransformationStep
import amf.core.validation.AMFValidationReport

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

private[amf] trait AMFEventListener {
  def notifyEvent(event: AMFEvent)
}
sealed trait AMFEvent {
  val name: String
}

@JSExportTopLevel("EventNames")
@JSExportAll
object AMFEventNames {
  val STARTING_PARSING             = "StartingParsing"
  val STARTING_CONTENT_PARSING     = "StartingContentParsing"
  val PARSED_SYNTAX                = "ParsedSyntax"
  val PARSED_MODEL                 = "ParsedModel"
  val FINISHED_PARSING             = "FinishedParsing"
  val STARTING_TRANSFORMATION      = "StartingTransformation"
  val FINISHED_TRANSFORMATION_STEP = "FinishedTransformationStep"
  val FINISHED_TRANSFORMATION      = "FinishedTransformation"
  val STARTING_VALIDATION          = "StartingValidation"
  val FINISHED_VALIDATION_PLUGIN   = "FinishedValidationPlugin"
  val FINISHED_VALIDATION          = "FinishedValidation"
  val STARTING_RENDERING           = "StartingRendering"
  val FINISHED_RENDERING_AST       = "FinishedRenderingAST"
  val FINISHED_RENDERING_SYNTAX    = "FinishedRenderingSyntax"
}

// Parsing Events

/**
  * every client invocation to the parsing logic
  * @param url URL of the top level document being parsed
  * @param mediaType optional media type passed in the invocation
  */
case class StartingParsingEvent(url: String, mediaType: Option[String]) extends AMFEvent {
  override val name: String = STARTING_PARSING
}

/**
  * called before parsing syntax of certain content.
  * @param url URL of the document being parsed
  * @param content original content that was parsed
  */
case class StartingContentParsingEvent(url: String, content: Content) extends AMFEvent {
  override val name: String = STARTING_CONTENT_PARSING
}

/**
  * every successful syntax AST being parsed for any document
  * @param url URL of the document being parsed
  * @param content original content that was parsed
  * @param parsedAST Parsed document AST
  */
case class ParsedSyntaxEvent(url: String, content: Content, parsedAST: ParsedDocument) extends AMFEvent {
  override val name: String = PARSED_SYNTAX
}

/**
  * every successful domain model being parsed for any document
  * @param url URL of the document being parsed
  * @param unit Parsed domain unit
  */
case class ParsedModelEvent(url: String, unit: BaseUnit) extends AMFEvent {
  override val name: String = PARSED_MODEL
}

/**
  * every successful parser invocation containing the top level domain unit being parsed
  * @param url URL of the top level document being parsed
  * @param unit parsed domain unit for the top level document
  */
case class FinishedParsingEvent(url: String, unit: BaseUnit) extends AMFEvent {
  override val name: String = FINISHED_PARSING
}

// Resolution Events

case class StartingTransformationEvent(pipeline: TransformationPipeline) extends AMFEvent {
  override val name: String = STARTING_TRANSFORMATION
}

case class FinishedTransformationStepEvent(stage: TransformationStep, index: Int) extends AMFEvent {
  override val name: String = FINISHED_TRANSFORMATION_STEP
}

case class FinishedTransformationEvent(unit: BaseUnit) extends AMFEvent {
  override val name: String = FINISHED_TRANSFORMATION
}

// Validation Events
// TODO missing invocation

case class StartingValidationEvent(totalPlugins: Int) extends AMFEvent {
  override val name: String = STARTING_VALIDATION
}

case class FinishedValidationPluginEvent(plugin: AMFValidatePlugin, result: ValidationResult) extends AMFEvent {
  override val name: String = FINISHED_VALIDATION_PLUGIN
}

case class FinishedValidationEvent(report: AMFValidationReport) extends AMFEvent {
  override val name: String = FINISHED_VALIDATION
}

// Rendering Events

case class StartingRenderingEvent(unit: BaseUnit, plugin: AMFRenderPlugin, mediaType: String) extends AMFEvent {
  override val name: String = STARTING_RENDERING
}

case class FinishedRenderingASTEvent(unit: BaseUnit, renderedAST: ParsedDocument) extends AMFEvent {
  override val name: String = FINISHED_RENDERING_AST
}

case class FinishedRenderingSyntaxEvent(unit: BaseUnit) extends AMFEvent {
  override val name: String = FINISHED_RENDERING_SYNTAX
}
