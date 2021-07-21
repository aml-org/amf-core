package amf.core.client.scala.model.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.ExternalSourceElementModel._
import amf.core.client.scala.model.StrField
import amf.core.internal.adoption.AdoptionDependantCalls

trait ExternalSourceElement extends DomainElement with AdoptionDependantCalls {

  def raw: StrField         = fields.field(Raw)         //we should set this while parsing
  def referenceId: StrField = fields.field(ReferenceId) /// only for graph parser logic

  override def location(): Option[String] = {

    val location: StrField = fields.field(Location)
    if (location.option().isDefined) location.option()
    else super.location()
  }

  // this its dynamic, because when graph emitter is going to serialize the raw field, first we need to check if its a link to an external fragment.
  // In that case the raw shouldn't be emitted, and it should be only a ref to the external domain element with the raw. This its to avoid duplicated json and xml schemas definitions
  // todo: antonio add comment.

  def isLinkToSource = fields.entry(ReferenceId).isDefined

  def withReference(id: String): this.type = set(ReferenceId, id)
}
