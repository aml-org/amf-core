package amf.client.remod

import amf.client.parse.DefaultParserErrorHandler
import amf.core.errorhandling.{AmfResultErrorHandler, ErrorHandler}

trait ErrorHandlerProvider {

  // Returns a new instance of error handler to collect results
  def errorHandler(): AmfResultErrorHandler
}

object DefaultErrorHandlerProvider extends ErrorHandlerProvider {
  override def errorHandler(): AmfResultErrorHandler = DefaultParserErrorHandler()
}
