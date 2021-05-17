package amf.client.handler

import amf.client.convert.CoreClientConverters._
import amf.client.resolve.ClientErrorHandler
import amf.client.validate.ValidationResult
import amf.core.parser

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel("ErrorHandler")
@JSExportAll
object ErrorHandler {

  def handler(obj: JsErrorHandler): ClientErrorHandler =
    new ClientErrorHandler {
      override def reportConstraint(id: String,
                                    node: String,
                                    property: ClientOption[String],
                                    message: String,
                                    range: ClientOption[parser.Range],
                                    level: String,
                                    location: ClientOption[String]): Unit =
        obj.reportConstraint(id, node, property, message, range, level, location)

      override def results(): ClientList[ValidationResult] = obj.results()
    }

}

@js.native
trait JsErrorHandler extends js.Object {

  def reportConstraint(id: String,
                       node: String,
                       property: ClientOption[String],
                       message: String,
                       range: ClientOption[amf.core.parser.Range],
                       level: String,
                       location: ClientOption[String]): Unit = js.native

  def results(): ClientList[ValidationResult] = js.native
}
