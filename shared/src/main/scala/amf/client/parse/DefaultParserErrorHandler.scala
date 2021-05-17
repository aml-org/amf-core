package amf.client.parse
import amf.core.errorhandling.{AmfResultErrorHandler, ErrorCollector, ErrorHandler}
import amf.core.parser.errorhandler.AmfParserErrorHandler
import amf.core.validation._

class DefaultParserErrorHandler() extends ErrorCollector with AmfParserErrorHandler {}

object DefaultParserErrorHandler {
  def apply(): DefaultParserErrorHandler = new DefaultParserErrorHandler()

  def fromErrorHandler(errorHandler: ErrorHandler): AmfParserErrorHandler = {
    errorHandler match {
      case parser: AmfParserErrorHandler => parser
      case eh: AmfResultErrorHandler =>
        new AmfParserErrorHandler {

          override def handlerAmfResult(result: AMFValidationResult): Boolean =
            eh.handlerAmfResult(result)

          override def results(): List[AMFValidationResult] = eh.results()
        }
      case eh: ErrorHandler =>
        new AmfParserErrorHandler {
          override def handlerAmfResult(result: AMFValidationResult): Boolean = {
            eh.reportConstraint(result.validationId,
                                result.targetNode,
                                result.targetProperty,
                                result.message,
                                result.position,
                                result.severityLevel,
                                result.location)
            true
          }

          override def results(): List[AMFValidationResult] = eh.results()
        }
    }
  }
}
