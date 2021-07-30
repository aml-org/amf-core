package amf.core.client.scala.config

import amf.core.client.common.remote.Content
import amf.core.client.platform.config.AMFEventNames._
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.ParsedDocument
import amf.core.client.scala.transform.{TransformationPipeline, TransformationStep}
import amf.core.client.scala.validation.AMFValidationReport
import amf.core.internal.plugins.render.AMFRenderPlugin
import amf.core.internal.plugins.validation.AMFValidatePlugin

// interface is duplicated in exported package to maintain separate hierarchy of AMFEvents

trait AMFEventListener {
  def notifyEvent(event: AMFEvent)
}

trait AMFEvent {
  val name: String
}

protected[amf] trait GroupedEvent { event: AMFEvent =>
  val groupKey: String
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Parsing Events ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

/**
  * every client invocation to the parsing logic
  * @param url URL of the top level document being parsed
  * @param mediaType optional media type passed in the invocation
  */
case class StartingParsingEvent(url: String, mediaType: Option[String]) extends AMFEvent with GroupedEvent {
  override val name: String     = StartedParse
  override val groupKey: String = url
}

/**
  * called before parsing syntax of certain content.
  * @param url URL of the document being parsed
  * @param content original content that was parsed
  */
case class StartingContentParsingEvent(url: String, content: Content) extends AMFEvent with GroupedEvent {
  override val name: String     = StartedContentParse
  override val groupKey: String = url
}

/**
  * every successful syntax AST being parsed for any document
  * @param url URL of the document being parsed
  * @param content original content that was parsed
  * @param parsedAST Parsed document AST
  */
case class ParsedSyntaxEvent(url: String, content: Content, parsedAST: ParsedDocument)
    extends AMFEvent
    with GroupedEvent {
  override val name: String     = ParsedSyntax
  override val groupKey: String = url
}

/**
  * every successful domain model being parsed for any document
  * @param url URL of the document being parsed
  * @param unit Parsed domain unit
  */
case class ParsedModelEvent(url: String, unit: BaseUnit) extends AMFEvent with GroupedEvent {
  override val name: String     = ParsedModel
  override val groupKey: String = url
}

case class SelectedParsePluginEvent(rootLocation: String, plugin: AMFParsePlugin) extends AMFEvent with GroupedEvent {
  override val name: String     = SelectedParsePlugin
  override val groupKey: String = rootLocation
}

case class FoundReferencesEvent(rootLocation: String, amount: Int) extends AMFEvent with GroupedEvent {
  override val name: String     = FoundReferences
  override val groupKey: String = rootLocation
}

case class DetectedSyntaxMediaTypeEvent(location: String, mediaType: String) extends AMFEvent with GroupedEvent {
  override val name: String     = DetectedSyntaxMediaType
  override val groupKey: String = location
}

/**
  * every successful parser invocation containing the top level domain unit being parsed
  * @param url URL of the top level document being parsed
  * @param unit parsed domain unit for the top level document
  */
case class FinishedParsingEvent(url: String, unit: BaseUnit) extends AMFEvent with GroupedEvent {
  override val name: String     = FinishedParse
  override val groupKey: String = url
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Transformation Events ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

case class StartingTransformationEvent(pipeline: TransformationPipeline) extends AMFEvent {
  override val name: String = StartedTransformation
}

case class StartedTransformationStepEvent(step: TransformationStep, index: Int) extends AMFEvent with GroupedEvent {
  override val name: String     = StartedTransformationStep
  override val groupKey: String = s"transform/step-$index"
}

case class FinishedTransformationStepEvent(step: TransformationStep, index: Int) extends AMFEvent with GroupedEvent {
  override val name: String     = FinishedTransformationStep
  override val groupKey: String = s"transform/step-$index"
}

case class FinishedTransformationEvent(unit: BaseUnit) extends AMFEvent {
  override val name: String = FinishedTransformation
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Validation Events ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

case class StartingValidationEvent(totalPlugins: Int) extends AMFEvent {
  override val name: String = StartingValidation
}

case class FinishedValidationPluginEvent(plugin: AMFValidatePlugin, result: AMFValidationReport) extends AMFEvent {
  override val name: String = FinishedValidationPlugin
}

case class FinishedValidationEvent(result: AMFValidationReport) extends AMFEvent {
  override val name: String = FinishedValidation
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Render Events ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

case class StartingRenderToWriterEvent(unit: BaseUnit, mediaType: String) extends AMFEvent with GroupedEvent {
  override val name: String     = StartedRenderToWriter
  override val groupKey: String = unit.id
}

case class StartingRenderingEvent(unit: BaseUnit, plugin: AMFRenderPlugin, mediaType: String)
    extends AMFEvent
    with GroupedEvent {
  override val name: String     = StartedRender
  override val groupKey: String = unit.id
}

case class FinishedRenderingASTEvent(unit: BaseUnit, renderedAST: ParsedDocument) extends AMFEvent with GroupedEvent {
  override val name: String     = FinishedASTRender
  override val groupKey: String = unit.id
}

case class FinishedRenderingSyntaxEvent(unit: BaseUnit) extends AMFEvent with GroupedEvent {
  override val name: String     = FinishedSyntaxRender
  override val groupKey: String = unit.id
}
