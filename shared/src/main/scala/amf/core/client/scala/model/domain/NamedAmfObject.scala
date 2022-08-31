package amf.core.client.scala.model.domain

import amf.core.client.scala.model.StrField
import amf.core.internal.metamodel.Field
import amf.core.internal.parser.domain.{Annotations, ScalarNode => ScalarNodeObj}
import org.yaml.model.YNode

/** All AmfObject supporting name
  */
trait NamedAmfObject extends AmfObject {

  protected def nameField: Field

  /** Return AmfObject name. */
  def name: StrField = fields.field(nameField)

  def withName(node: YNode): this.type = withName(ScalarNodeObj(node))

  def withName(name: String, a: Annotations): this.type = set(nameField, AmfScalar(name, a), Annotations.inferred())

  /** Update AmfObject name. */
  def withName(nameNode: ScalarNodeObj): this.type = set(nameField, nameNode.text(), Annotations.inferred())

  def withName(name: String): this.type = withName(ScalarNodeObj(YNode(name)))

  def withSynthesizeName(name: String): this.type = set(nameField, AmfScalar(name), Annotations.synthesized())

}
