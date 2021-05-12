package amf.client.interface

import amf.client.resolve.ClientErrorHandler
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait ErrorHandlerProvider {
  // Returns a new instance of error handler to collect results
  def errorHandler(): ClientErrorHandler
}
