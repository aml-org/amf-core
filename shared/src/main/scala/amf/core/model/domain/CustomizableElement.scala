package amf.core.model.domain

import amf.core.model.domain.extensions.DomainExtension

trait CustomizableElement extends AmfObject {

  def customDomainProperties: Seq[DomainExtension]
  def withCustomDomainProperties(extensions: Seq[DomainExtension]): this.type
  def withCustomDomainProperty(extensions: DomainExtension): this.type
}
