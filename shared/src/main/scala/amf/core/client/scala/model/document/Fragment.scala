package amf.core.client.scala.model.document

import amf.core.client.scala.model.domain.{AmfObject, DomainElement}
import amf.core.internal.metamodel.document.{DocumentModel, FragmentModel}

/** Fragments: Units encoding domain fragments
  */
trait Fragment extends BaseUnit with EncodesModel {

  /** Returns the list document URIs referenced from the document that has been parsed to generate this model */
  override def references: Seq[BaseUnit] = fields(DocumentModel.References)

  override def encodes: DomainElement = fields(FragmentModel.Encodes)

  override def meta: FragmentModel = FragmentModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = ""

}

trait EncodesModel extends AmfObject {

  /** Encoded DomainElement described in the document element. */
  def encodes: DomainElement

  def withEncodes(encoded: DomainElement): this.type = set(FragmentModel.Encodes, encoded)
}
