package amf.core.client.platform.validation

import amf.core.client.scala.validation.{AMFValidationResult => InternalValidationResult}
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.convert.CoreClientConverters._
import amf.core.internal.utils.AmfStrings
import org.mulesoft.common.client.lexical.PositionRange

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class AMFValidationResult(private[amf] val _internal: InternalValidationResult) {

  @JSExportTopLevel("AMFValidationResult")
  def this(message: String,
           level: String,
           targetNode: String,
           targetProperty: String,
           validationId: String,
           range: PositionRange,
           location: String) =
    this(
        InternalValidationResult(message,
                                 level,
                                 targetNode,
                                 targetProperty.option,
                                 validationId,
                                 Some(LexicalInformation(range)),
                                 location.option,
                                 null))

  def message: String        = _internal.message
  def severityLevel: String  = _internal.severityLevel
  def targetNode: String     = _internal.targetNode
  def targetProperty: String = _internal.targetProperty.orNull
  def validationId: String   = _internal.validationId
  def source: Any            = _internal.source

  def position: PositionRange = _internal.position match {
    case Some(lexical) => lexical.range
    case _             => PositionRange.NONE
  }

  def location: ClientOption[String] = _internal.location.asClient
}
