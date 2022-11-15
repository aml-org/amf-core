package amf.core.client.scala.model.document

import amf.core.internal.metamodel.document.DocumentModel.{Declares => _, Location => _, References => _, Usage => _}
import amf.core.internal.metamodel.document.ModuleModel
import amf.core.internal.metamodel.document.ModuleModel._
import amf.core.internal.metamodel.domain.DomainElementModel.CustomDomainProperties
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.model.domain.{AmfObject, CustomizableElement, DomainElement}
import amf.core.internal.parser.domain.Fields
import amf.core.internal.parser.domain.{Annotations, Fields}
import org.yaml.model.YDocument

/** Module model class */
case class Module(fields: Fields, annotations: Annotations)
    extends BaseUnit
    with DeclaresModel
    with CustomizableElement {

  override def typeIris: Seq[String] = meta.typeIris

  /** Returns the list document URIs referenced from the document that has been parsed to generate this model */
  override def references: Seq[BaseUnit] = fields(References)

  /** Declared DomainElements that can be re-used from other documents. */
  override def declares: Seq[DomainElement] = fields(Declares)

  /** Meta data for the document */
  override def meta: ModuleModel = ModuleModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = ""

  def customDomainProperties: Seq[DomainExtension] = fields.field(CustomDomainProperties)

  def withCustomDomainProperties(extensions: Seq[DomainExtension]): this.type =
    setArray(CustomDomainProperties, extensions)

  def withCustomDomainProperty(extensions: DomainExtension): this.type = add(CustomDomainProperties, extensions)
}

trait DeclaresModel extends AmfObject {

  /** Declared DomainElements that can be re-used from other documents. */
  def declares: Seq[DomainElement]

  def withDeclares(declarations: Seq[DomainElement], annotations: Annotations = Annotations()): this.type =
    setArrayWithoutId(Declares, declarations, annotations)

  def withDeclaredElement(element: DomainElement): this.type = add(Declares, element)
}

object Module {
  def apply(): Module = apply(Annotations())

  def apply(ast: YDocument): Module = apply(Annotations(ast))

  def apply(annotations: Annotations): Module = apply(Fields(), annotations)
}
