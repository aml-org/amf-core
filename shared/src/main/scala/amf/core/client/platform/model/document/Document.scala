package amf.core.client.platform.model.document

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.platform.model.domain.DomainElement
import amf.core.client.scala.model.document.{Document => InternalDocument}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Document model class.
  */
@JSExportAll
class Document(private[amf] val _internal: InternalDocument) extends BaseUnit with EncodesModel with DeclaresModel {

  @JSExportTopLevel("model.document.Document")
  def this() = this(InternalDocument())

  @JSExportTopLevel("model.document.Document")
  def this(encoding: DomainElement) = this(InternalDocument().withEncodes(encoding))
}