package amf.core.client.platform.model.domain

import amf.core.client.platform.model.{AmfObjectWrapper, BoolField}
import amf.core.client.scala.model.domain.{DomainElement => InternalDomainElement}
import amf.core.internal.convert.CoreClientConverters._
import amf.core.internal.unsafe.PlatformSecrets
import org.mulesoft.common.client.lexical.PositionRange

import scala.scalajs.js.annotation.JSExportAll

/** Domain element.
  */
@JSExportAll
trait DomainElement extends AmfObjectWrapper with PlatformSecrets with CustomizableElement {

  override private[amf] val _internal: InternalDomainElement

  override def customDomainProperties: ClientList[DomainExtension] = _internal.customDomainProperties.asClient
  def extendsNode: ClientList[DomainElement]                       = _internal.extend.asClient
  def id: String                                                   = _internal.id
  def position: PositionRange                                      = _internal.position().map(_.range).orNull

  override def withCustomDomainProperties(extensions: ClientList[DomainExtension]): this.type = {
    _internal.withCustomDomainProperties(extensions.asInternal)
    this
  }

  def withExtendsNode(extension: ClientList[ParametrizedDeclaration]): this.type = {
    _internal.withExtends(extension.asInternal)
    this
  }

  def withId(id: String): this.type = {
    _internal.withId(id)
    this
  }

  def isExternalLink: BoolField = _internal.isExternalLink
  def withIsExternalLink(isExternalLink: Boolean): DomainElement = {
    _internal.withIsExternalLink(isExternalLink)
    this
  }

  def graph(): Graph = _internal.graph
}
