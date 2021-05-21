package amf.core.model.document

import amf.core.metamodel.{Field, Obj}
import amf.core.metamodel.document.AmfProcessingDataModel
import amf.core.metamodel.document.AmfProcessingDataModel.{
  GraphDependencies,
  ModelVersion,
  SourceVendor,
  Transformations
}
import amf.core.model.StrField
import amf.core.model.domain.{AmfObject, AmfScalar}
import amf.core.parser.{Annotations, Fields}

case class AmfProcessingData(fields: Fields, annotations: Annotations) extends AmfObject {

  def transformations: Seq[StrField]   = fields.field(Transformations)
  def sourceVendor: StrField           = fields.field(SourceVendor)
  def modelVersion: StrField           = fields.field(ModelVersion)
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)

  def transformedBy(id: String): this.type                = add(Transformations, AmfScalar(id))
  def withSourceVendor(mediatype: String): this.type      = set(SourceVendor, mediatype)
  def withGraphDependencies(deps: Seq[String]): this.type = set(GraphDependencies, deps)
  def withModelVersion(version: String): this.type        = set(ModelVersion, version)

  override def meta: Obj = AmfProcessingDataModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = "/unitActions"
}

object AmfProcessingData {
  def apply(): AmfProcessingData = AmfProcessingData(Fields(), Annotations())
}
