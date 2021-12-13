package amf.core.client.scala.model.domain.extensions

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel.{DefinedBy, Element, Extension, Name}
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.{DataNode, NamedDomainElement, Shape}
import amf.core.internal.parser.domain.Fields
import amf.core.internal.parser.domain.{Annotations, Fields}
import org.yaml.model.YPart

case class DomainExtension(fields: Fields, annotations: Annotations) extends Extension with NamedDomainElement {

  def definedBy: CustomDomainProperty = fields.field(DefinedBy)
  def obtainSchema: Shape             = definedBy.schema
  def element: StrField               = fields.field(Element)

  def withDefinedBy(customProperty: CustomDomainProperty): this.type = set(DefinedBy, customProperty)
  def withExtension(extension: DataNode): this.type                  = set(Extension, extension)
  def withElement(element: String): this.type                        = set(Element, element)

  def isScalarExtension: Boolean = !element.isNullOrEmpty

  def meta: DomainExtensionModel = DomainExtensionModel

  // This element will never be serialised in the JSON-LD graph, it is just a placeholder
  // for the extension point. ID is not required for serialisation

  /** Value , path + field value that is used to compose the id when the object its adopted */
  private[amf] override def componentId: String = customDomainPropertyName.getOrElse("/extension")

  private def customDomainPropertyName: Option[String] = Option(definedBy).flatMap(_.name.option()).map(x => s"/$x")

  /** Call after object has been adopted by specified parent. */
  override def adopted(parent: String, cycle: Seq[String] = Seq()): this.type = {
    if (Option(id).isEmpty || id.startsWith("null/")) simpleAdoption(parent)
    this
  }

  override protected def nameField: Field = Name
}

object DomainExtension {
  def apply(): DomainExtension = apply(Annotations())

  def apply(ast: YPart): DomainExtension = apply(Annotations(ast))

  def apply(annotations: Annotations): DomainExtension = new DomainExtension(Fields(), annotations)
}
