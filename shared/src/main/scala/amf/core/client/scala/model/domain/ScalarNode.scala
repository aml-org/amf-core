package amf.core.client.scala.model.domain

import amf.core.client.scala.model.domain.ScalarNode.forDataType
import amf.core.client.scala.model.domain.common.DescribedElement
import amf.core.client.scala.model.{DataType, StrField}
import amf.core.client.scala.model.domain.templates.Variable
import amf.core.internal.annotations.ScalarType
import amf.core.internal.metamodel.domain.ScalarNodeModel
import amf.core.internal.metamodel.domain.ScalarNodeModel._
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.internal.transform.VariableReplacer
import org.yaml.model.YPart

/** Scalar values with associated data type
  */
class ScalarNode(override val fields: Fields, val annotations: Annotations)
    extends DataNode(annotations)
    with DescribedElement {

  def withValue(v: String): this.type = withValue(v, Annotations())

  def withValue(v: String, ann: Annotations): this.type =
    set(Value, AmfScalar(v, ann), ann)

  def withDataType(dataType: String): this.type =
    set(ScalarNodeModel.DataType, forDataType(dataType))

  def withDataType(dataType: String, ann: Annotations): this.type =
    set(ScalarNodeModel.DataType, AmfScalar(dataType, ann), ann)

  def value: StrField = fields.field(Value)

  def dataType: StrField = fields.field(ScalarNodeModel.DataType)

  override def meta: ScalarNodeModel.type = ScalarNodeModel

  override def replaceVariables(values: Set[Variable], keys: Seq[ElementTree])(
      reportError: String => Unit
  ): DataNode = {
    VariableReplacer.replaceNodeVariables(this, values, reportError)
  }
  override def copyNode(): DataNode =
    new ScalarNode(fields.copy(), annotations.copy()).withId(id)

}

object ScalarNode {
  def apply(): ScalarNode = apply("", None)

  def apply(annotations: Annotations): ScalarNode = apply("", None, annotations)

  def apply(value: String, dataType: Option[String]): ScalarNode =
    apply(value, dataType, Annotations())

  def apply(value: String, dataType: Option[String], ast: YPart): ScalarNode =
    apply(value, dataType, Annotations(ast))

  def apply(value: String, dataType: Option[String], annotations: Annotations): ScalarNode = {
    val scalar = new ScalarNode(Fields(), annotations)
    dataType.foreach(d => {
      annotations += ScalarType(d)
      scalar.withDataType(d)
    })
    scalar.set(Value, AmfScalar(value, annotations), Annotations.inferred())
  }

  def forDataType(dataTypeUri: String): AmfScalar = dataTypeUri match {
    case DataType.String       => string
    case DataType.Integer      => integer
    case DataType.Number       => number
    case DataType.Long         => long
    case DataType.Double       => double
    case DataType.Float        => float
    case DataType.Decimal      => decimal
    case DataType.Boolean      => boolean
    case DataType.Date         => date
    case DataType.Time         => time
    case DataType.DateTime     => dateTime
    case DataType.DateTimeOnly => dateTimeOnly
    case DataType.File         => file
    case DataType.Byte         => byte
    case DataType.Binary       => base64Binary
    case DataType.Password     => password
    case DataType.Any          => anyType
    case DataType.AnyUri       => anyURI
    case DataType.Nil          => nil
    case _                     => AmfScalar(dataTypeUri, Annotations())
  }

  private val string       = AmfScalar(DataType.String, Annotations.empty)
  private val integer      = AmfScalar(DataType.Integer, Annotations.empty)
  private val number       = AmfScalar(DataType.Number, Annotations.empty)
  private val long         = AmfScalar(DataType.Long, Annotations.empty)
  private val double       = AmfScalar(DataType.Double, Annotations.empty)
  private val float        = AmfScalar(DataType.Float, Annotations.empty)
  private val decimal      = AmfScalar(DataType.Decimal, Annotations.empty)
  private val boolean      = AmfScalar(DataType.Boolean, Annotations.empty)
  private val date         = AmfScalar(DataType.Date, Annotations.empty)
  private val time         = AmfScalar(DataType.Time, Annotations.empty)
  private val dateTime     = AmfScalar(DataType.DateTime, Annotations.empty)
  private val dateTimeOnly = AmfScalar(DataType.DateTimeOnly, Annotations.empty)
  private val file         = AmfScalar(DataType.File, Annotations.empty)
  private val byte         = AmfScalar(DataType.Byte, Annotations.empty)
  private val base64Binary = AmfScalar(DataType.Binary, Annotations.empty)
  private val password     = AmfScalar(DataType.Password, Annotations.empty)
  private val anyType      = AmfScalar(DataType.Any, Annotations.empty)
  private val anyURI       = AmfScalar(DataType.AnyUri, Annotations.empty)
  private val nil          = AmfScalar(DataType.Nil, Annotations.empty)
}
