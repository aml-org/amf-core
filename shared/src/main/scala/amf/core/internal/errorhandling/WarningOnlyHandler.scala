package amf.core.internal.errorhandling

import amf.core.client.common.validation.SeverityLevels
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.validation.AMFValidationResult
import amf.core.internal.validation.CoreValidations.SyamlWarning
import org.mulesoft.lexer.SourceLocation
import org.yaml.model.{SyamlException, YError}

case class WarningOnlyHandler(parent: AMFErrorHandler) extends RuntimeWrapperErrorHandler(parent: AMFErrorHandler) {

  override def report(result: AMFValidationResult): Unit = {
    super.report(result.copy(severityLevel = SeverityLevels.WARNING))
    warningRegister = true
  }

  private var warningRegister: Boolean = false

  def hasRegister: Boolean = warningRegister
}
