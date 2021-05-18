package amf.client.remod

import amf.client.parse.DefaultErrorHandler
import amf.core.errorhandling.AMFErrorHandler

trait ErrorHandlerProvider {

  // Returns a new instance of error handler to collect results
  def errorHandler(): AMFErrorHandler
}

object DefaultErrorHandlerProvider extends ErrorHandlerProvider {
  override def errorHandler(): AMFErrorHandler = DefaultErrorHandler()
}
