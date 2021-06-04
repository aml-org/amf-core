package amf.plugins.domain.apicontract.models

import amf.core.metamodel.{Field, Obj}
import amf.core.model.domain.{DomainElement, Linkable, NamedDomainElement, Shape}
import amf.core.model.{BoolField, StrField}
import amf.core.parser.{Annotations, Fields}
import amf.plugins.domain.shapes.models.{CreativeWork, Example, ExemplifiedDomainElement, NodeShape}
import amf.plugins.domain.apicontract.metamodel.MessageModel
import amf.plugins.domain.apicontract.metamodel.MessageModel._
import amf.plugins.domain.apicontract.models.bindings.{MessageBinding, MessageBindings}
import amf.core.utils.AmfStrings
import amf.plugins.domain.shapes.metamodel.common.ExamplesField.Examples

class Message(override val fields: Fields, override val annotations: Annotations)
    extends NamedDomainElement
    with ExemplifiedDomainElement
    with Linkable {

  def description: StrField        = fields.field(Description)
  def isAbstract: BoolField        = fields.field(IsAbstract)
  def documentation: CreativeWork  = fields.field(Documentation)
  def tags: Seq[Tag]               = fields.field(Tags)
  def payloads: Seq[Payload]       = fields.field(Payloads)
  def correlationId: CorrelationId = fields.field(MessageModel.CorrelationId)
  def displayName: StrField        = fields.field(DisplayName)
  def title: StrField              = fields.field(Title)
  def summary: StrField            = fields.field(Summary)
  def bindings: MessageBindings    = fields.field(Bindings)
  def headerExamples: Seq[Example] = fields.field(HeaderExamples)
  def headers: Seq[Parameter]      = fields.field(Headers)
  def headerSchema: NodeShape      = fields.field(HeaderSchema)

  def withDescription(description: String): this.type            = set(Description, description)
  def isAbstract(isAbstract: Boolean): this.type                 = set(IsAbstract, isAbstract)
  def withDocumentation(documentation: CreativeWork): this.type  = set(Documentation, documentation)
  def withTags(tags: Seq[Tag]): this.type                        = setArray(Tags, tags)
  def withPayloads(payloads: Seq[Payload]): this.type            = setArray(Payloads, payloads)
  def withCorrelationId(correlationId: CorrelationId): this.type = set(MessageModel.CorrelationId, correlationId)
  def withDisplayName(displayName: String): this.type            = set(DisplayName, displayName)
  def withTitle(title: String): this.type                        = set(Title, title)
  def withSummary(summary: String): this.type                    = set(Summary, summary)
  def withBindings(bindings: MessageBindings): this.type         = set(Bindings, bindings)
  def withHeaders(headers: Seq[Parameter]): this.type            = setArray(Headers, headers)
  def withHeaderExamples(examples: Seq[Example]): this.type      = setArray(HeaderExamples, examples)
  def withHeaderSchema(obj: NodeShape): this.type                = set(HeaderSchema, obj)

  def withPayload(mediaType: Option[String] = None): Payload = {
    val result = Payload()
    mediaType.map(result.withMediaType)
    add(Payloads, result)
    result
  }

  override def meta: MessageModel = MessageModel

  override def linkCopy(): Message = Message().withId(id)

  override def nameField: Field = MessageModel.Name

  override def componentId: String = "/" + name.option().getOrElse("message").urlComponentEncoded

  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement =
    (fields, annot) => new Message(fields, annot)
}

object Message {

  def apply(): Message = apply(Annotations())

  def apply(annotations: Annotations): Message = new Message(Fields(), annotations)
}
