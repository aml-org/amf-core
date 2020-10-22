package amf.core.model.domain

import amf.core.metamodel.Field
import amf.core.model.StrField
import amf.core.parser.{Annotations, ScalarNode => ScalarNodeObj}
import org.yaml.model.YNode

/**
  * All DomainElements supporting name
  */
trait NamedDomainElement extends DomainElement {

  protected def nameField: Field

  /** Return DomainElement name. */
  def name: StrField = fields.field(nameField)

  def withName(node: YNode): this.type = withName(ScalarNodeObj(node))

  def withName(name:String, a:Annotations): this.type = set(nameField, AmfScalar(name, a), Annotations.inferred())

  /** Update DomainElement name. */
  def withName(nameNode: ScalarNodeObj): this.type = set(nameField, nameNode.text(), Annotations.inferred())

  def withSynthesizeName(name:String): this.type = set(nameField, AmfScalar(name), Annotations.synthesized())


}
