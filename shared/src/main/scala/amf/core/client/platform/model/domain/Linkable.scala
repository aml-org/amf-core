package amf.core.client.platform.model.domain

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.platform.model.StrField
import amf.core.client.scala.model.domain.{DomainElement => InternalDomainElement, Linkable => InternalLinkable}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait Linkable { this: DomainElement with Linkable =>

  private[amf] def _internal: InternalDomainElement with InternalLinkable

  def linkTarget: ClientOption[DomainElement] = _internal.linkTarget.asClient

  def isLink: Boolean     = _internal.linkTarget.isDefined
  def linkLabel: StrField = _internal.linkLabel

  def linkCopy(): Linkable

  def withLinkTarget(target: DomainElement with Linkable): this.type = {
    _internal.withLinkTarget(target._internal)
    this
  }

  def withLinkLabel(label: String): this.type = {
    _internal.withLinkLabel(label)
    this
  }

  def link[T](): T = {
    val copy = linkCopy().withLinkTarget(this)
    copy.asInstanceOf[T]
  }

  def link[T](label: String): T = {
    val copy = linkCopy().withLinkTarget(this)
    copy.withLinkLabel(label)
    copy.asInstanceOf[T]
  }
}
