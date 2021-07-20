package amf.core.client.scala.model.domain.extensions

import amf.core.client.scala.model.{IntField, StrField}
import amf.core.client.scala.model.domain._
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.extensions.CustomDomainPropertyModel
import amf.core.internal.metamodel.domain.extensions.CustomDomainPropertyModel._
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.internal.utils._
import org.yaml.model.YPart

case class CustomDomainProperty(fields: Fields, annotations: Annotations)
    extends DomainElement
    with Linkable
    with NamedDomainElement {

  def displayName: StrField = fields.field(DisplayName)
  def description: StrField = fields.field(Description)
  def domain: Seq[StrField] = fields.field(Domain)
  def schema: Shape         = fields.field(Schema)
  def serializationOrder: IntField = fields.field(SerializationOrder)

  def withSerializationOrder(order: Int): this.type = set(SerializationOrder, order)
  def withDisplayName(displayName: String): this.type = set(DisplayName, displayName)
  def withDescription(description: String): this.type = set(Description, description)
  def withDomain(domain: Seq[String]): this.type      = set(Domain, domain)
  def withSchema(schema: Shape): this.type            = set(Schema, schema)

  override def adopted(parent: String, cycle: Seq[String] = Seq()): this.type =
    if (Option(this.id).isEmpty) {
      if (parent.contains("#")) {
        withId(parent + "/" + componentId.urlComponentEncoded)
      } else {
        withId(parent + "#" + componentId.urlComponentEncoded)
      }
    } else { this }

  override def linkCopy(): CustomDomainProperty = CustomDomainProperty().withId(id)

  override def meta = CustomDomainPropertyModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  private[amf] override def componentId: String = name.option().map(_.urlComponentEncoded).getOrElse("")

  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement =
    CustomDomainProperty.apply
  override def nameField: Field = Name
}

object CustomDomainProperty {
  def apply(): CustomDomainProperty = apply(Annotations())

  def apply(ast: YPart): CustomDomainProperty = apply(Annotations(ast))

  def apply(annotations: Annotations): CustomDomainProperty = CustomDomainProperty(Fields(), annotations)
}
