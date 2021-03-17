package amf.client.remod

import amf.client.parse.DefaultParserErrorHandler
import amf.core.errorhandling.ErrorHandler

private[amf] trait ErrorHandlerProvider {

  // Returns a new instance of error handler to collect results
  def errorHandler(): ErrorHandler
}

object DefaultErrorHandlerProvider extends ErrorHandlerProvider {
  override def errorHandler(): ErrorHandler = DefaultParserErrorHandler()
}
