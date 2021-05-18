package amf.core.parser.errorhandler
import amf.core.errorhandling.AMFErrorHandler
import amf.core.validation.AMFValidationResult

abstract class RuntimeWrapperErrorHandler(parent: AMFErrorHandler) extends AMFErrorHandler {

  override def report(result: AMFValidationResult): Unit = parent.report(result)

}
