package amf.client.validate

import amf.ProfileName
import amf.client.convert.CoreClientConverters._
import amf.core.validation.{AMFValidationReport => InternalValidationReport}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class AMFValidationReport(private[amf] val _internal: InternalValidationReport) {

  @JSExportTopLevel("client.validate.AMFValidationReport")
  def this(model: String, profile: ProfileName, results: ClientList[ValidationResult]) =
    this(InternalValidationReport(model, profile, results.asInternal))

  def conforms: Boolean                     = _internal.conforms
  def model: String                         = _internal.model
  def profile: ProfileName                  = _internal.profile
  def results: ClientList[ValidationResult] = _internal.results.asClient

  override def toString: String = _internal.toString

  def toString(max: Int): String = _internal.toString(max)
}
