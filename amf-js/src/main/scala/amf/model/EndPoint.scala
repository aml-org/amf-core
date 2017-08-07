package amf.model

import amf.model.builder.EndPointBuilder

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExportAll

/**
  * EndPoints js class
  */
@JSExportAll
case class EndPoint private[model] (private val endPoint: amf.domain.EndPoint) extends DomainElement {

  val name: String = endPoint.name

  val description: String = endPoint.description

  val path: String = endPoint.path

  val operations: js.Iterable[Operation] = endPoint.operations.map(Operation).toJSArray

  val parameters: js.Iterable[Parameter] = endPoint.parameters.map(Parameter).toJSArray

  def toBuilder: EndPointBuilder = EndPointBuilder(endPoint.toBuilder)
}
