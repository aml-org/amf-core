package amf.core.internal.errorhandling

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.validation.CoreValidations.SyamlWarning
import org.mulesoft.lexer.SourceLocation
import org.yaml.model.{SyamlException, YError}

case class WarningOnlyHandler(parent: AMFErrorHandler) extends RuntimeWrapperErrorHandler(parent: AMFErrorHandler) {

  /*
  override def handle(location: SourceLocation, e: SyamlException): Unit = {
    warning(SyamlWarning, "", e.getMessage, location)
    warningRegister = true
  }

  override def handle[T](error: YError, defaultValue: T): T = {
    warning(SyamlWarning, "", error.error, part(error))
    warningRegister = true
    defaultValue
  }
  */

  private var warningRegister: Boolean = false

  def hasRegister: Boolean = warningRegister
}
