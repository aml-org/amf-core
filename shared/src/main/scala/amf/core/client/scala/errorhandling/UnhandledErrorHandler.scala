package amf.core.client.scala.errorhandling

import amf.core.client.scala.validation.AMFValidationResult
import amf.core.internal.annotations.LexicalInformation

trait UnhandledErrorHandler extends AMFErrorHandler {

  override def report(result: AMFValidationResult): Unit =
    throw new Exception(
        s"  Message: ${result.message}\n  Target: ${result.targetNode}\nProperty: ${result.targetProperty
          .getOrElse("")}\n  Position: ${result.position}\n at location: ${result.location}")
}

object UnhandledErrorHandler extends UnhandledErrorHandler {}
