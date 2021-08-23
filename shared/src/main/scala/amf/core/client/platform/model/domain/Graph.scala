package amf.core.client.platform.model.domain

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.scala.model.domain.{Graph => InternalGraph}
import amf.core.client.scala.vocabulary.Namespace

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
case class Graph(private[amf] val _internal: InternalGraph) {
  def types(): ClientList[String] = _internal.types().asClient

  def properties(): ClientList[String] = _internal.properties().asClient

  def containsProperty(uri: String): Boolean = _internal.containsProperty(uri)

  def getObjectByProperty(uri: String): ClientList[DomainElement] = _internal.getObjectByProperty(uri).asClient

  def scalarByProperty(uri: String): ClientList[Any] = _internal.scalarByProperty(uri).asClient

}
