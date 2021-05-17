package amf.client.resolve
import amf.client.convert.BidirectionalMatcher
import amf.client.convert.CoreClientConverters._
import amf.client.validate.ValidationResult
import amf.core.annotations.LexicalInformation
import amf.core.errorhandling.ErrorHandler
import amf.core.parser
import amf.core.validation.AMFValidationResult

object ClientErrorHandlerConverter {

  implicit object RangeToLexicalConverter extends BidirectionalMatcher[LexicalInformation, amf.core.parser.Range] {

    override def asInternal(from: amf.core.parser.Range): LexicalInformation = LexicalInformation(from)
    override def asClient(from: LexicalInformation): amf.core.parser.Range   = from.range
  }

  implicit object ErrorHandlerConverter extends BidirectionalMatcher[ErrorHandler, ClientErrorHandler] {

    override def asInternal(from: ClientErrorHandler): ErrorHandler = convert(from)
    override def asClient(from: ErrorHandler): ClientErrorHandler   = convertToClient(from)
  }

  def convert(clientErrorHandler: ClientErrorHandler): ErrorHandler =
    new ErrorHandler {
      override def reportConstraint(id: String,
                                    node: String,
                                    property: Option[String],
                                    message: String,
                                    range: Option[LexicalInformation],
                                    level: String,
                                    location: Option[String]): Unit = {
        clientErrorHandler.reportConstraint(id,
                                            node,
                                            property.asClient,
                                            message,
                                            range.asClient,
                                            level,
                                            location.asClient)
      }

      override def results(): List[AMFValidationResult] = clientErrorHandler.results().asInternal.toList
    }

  def convertToClient(errorHandler: ErrorHandler): ClientErrorHandler =
    new ClientErrorHandler {
      override def reportConstraint(id: String,
                                    node: String,
                                    property: ClientOption[String],
                                    message: String,
                                    range: ClientOption[parser.Range],
                                    level: String,
                                    location: ClientOption[String]): Unit = {
        errorHandler.reportConstraint(id,
                                      node,
                                      property.toScala,
                                      message,
                                      range.toScala.map(RangeToLexicalConverter.asInternal),
                                      level,
                                      location.toScala)
      }

      override def results(): ClientList[ValidationResult] = errorHandler.results().asClient
    }
}
