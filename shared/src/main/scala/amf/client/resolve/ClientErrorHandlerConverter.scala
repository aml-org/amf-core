package amf.client.resolve
import amf.client.convert.CoreClientConverters._
import amf.client.convert.{BidirectionalMatcher, CoreClientConverters}
import amf.client.validate.ValidationResult
import amf.core.annotations.LexicalInformation
import amf.core.errorhandling.AMFErrorHandler
import amf.core.validation.AMFValidationResult

object ClientErrorHandlerConverter {

  implicit object RangeToLexicalConverter extends BidirectionalMatcher[LexicalInformation, amf.core.parser.Range] {

    override def asInternal(from: amf.core.parser.Range): LexicalInformation = LexicalInformation(from)
    override def asClient(from: LexicalInformation): amf.core.parser.Range   = from.range
  }

  implicit object ErrorHandlerConverter extends BidirectionalMatcher[AMFErrorHandler, ClientErrorHandler] {

    override def asInternal(from: ClientErrorHandler): AMFErrorHandler = convert(from)
    override def asClient(from: AMFErrorHandler): ClientErrorHandler   = convertToClient(from)
  }

  def convert(clientErrorHandler: ClientErrorHandler): AMFErrorHandler =
    new AMFErrorHandler {

      override def report(result: AMFValidationResult): Unit = clientErrorHandler.report(result)
      override def getResults(): List[AMFValidationResult]   = clientErrorHandler.getResults.asInternal.toList
    }

  def convertToClient(errorHandler: AMFErrorHandler): ClientErrorHandler =
    new ClientErrorHandler {
      override def getResults: CoreClientConverters.ClientList[ValidationResult] = errorHandler.getResults.asClient

      override def report(result: ValidationResult): Unit = errorHandler.report(result)
    }
}
