package amf.client.remod.amfcore.config

import amf.client.exported.config.AMFEvent
import amf.client.exported.config.AMFEventNames._
import amf.client.remod.amfcore.plugins.render.AMFRenderPlugin
import amf.client.remod.amfcore.plugins.validate.{AMFValidatePlugin, ValidationResult}
import amf.client.remote.Content
import amf.core.model.document.BaseUnit
import amf.core.parser.ParsedDocument
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.TransformationStep
import amf.core.validation.AMFValidationReport

// Parsing Events

/**
  * every client invocation to the parsing logic
  * @param url URL of the top level document being parsed
  * @param mediaType optional media type passed in the invocation
  */
case class StartingParsingEvent(url: String, mediaType: Option[String]) extends AMFEvent {
  override val name: String = StartingParsing
}

/**
  * called before parsing syntax of certain content.
  * @param url URL of the document being parsed
  * @param content original content that was parsed
  */
case class StartingContentParsingEvent(url: String, content: Content) extends AMFEvent {
  override val name: String = StartingContentParsing
}

/**
  * every successful syntax AST being parsed for any document
  * @param url URL of the document being parsed
  * @param content original content that was parsed
  * @param parsedAST Parsed document AST
  */
case class ParsedSyntaxEvent(url: String, content: Content, parsedAST: ParsedDocument) extends AMFEvent {
  override val name: String = ParsedSyntax
}

/**
  * every successful domain model being parsed for any document
  * @param url URL of the document being parsed
  * @param unit Parsed domain unit
  */
case class ParsedModelEvent(url: String, unit: BaseUnit) extends AMFEvent {
  override val name: String = ParsedModel
}

/**
  * every successful parser invocation containing the top level domain unit being parsed
  * @param url URL of the top level document being parsed
  * @param unit parsed domain unit for the top level document
  */
case class FinishedParsingEvent(url: String, unit: BaseUnit) extends AMFEvent {
  override val name: String = FinishedParsing
}

// Resolution Events

case class StartingTransformationEvent(pipeline: TransformationPipeline) extends AMFEvent {
  override val name: String = StartingTransformation
}

case class FinishedTransformationStepEvent(step: TransformationStep, index: Int) extends AMFEvent {
  override val name: String = FinishedTransformationStep
}

case class FinishedTransformationEvent(unit: BaseUnit) extends AMFEvent {
  override val name: String = FinishedTransformation
}

// Validation Events
// TODO missing invocation

case class StartingValidationEvent(totalPlugins: Int) extends AMFEvent {
  override val name: String = StartingValidation
}

case class FinishedValidationPluginEvent(plugin: AMFValidatePlugin, result: AMFValidationReport) extends AMFEvent {
  override val name: String = FinishedValidationPlugin
}

case class FinishedValidationEvent(result: AMFValidationReport) extends AMFEvent {
  override val name: String = FinishedValidation
}

// Rendering Events

case class StartingRenderingEvent(unit: BaseUnit, plugin: AMFRenderPlugin, mediaType: String) extends AMFEvent {
  override val name: String = StartingRendering
}

case class FinishedRenderingASTEvent(unit: BaseUnit, renderedAST: ParsedDocument) extends AMFEvent {
  override val name: String = FinishedRenderingAST
}

case class FinishedRenderingSyntaxEvent(unit: BaseUnit) extends AMFEvent {
  override val name: String = FinishedRenderingSyntax
}
