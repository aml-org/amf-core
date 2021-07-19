package amf.core.client.scala.model.domain

import amf.core.internal.metamodel.{Field, ModelDefaultBuilder, Obj}
import amf.core.internal.parser.domain.{Annotations, Fields}

import scala.collection.mutable

/**
  * Created by pedro.colunga on 8/15/17.
  */
trait AmfObject extends AmfElement {

  def meta: Obj

  /** Set of fields composing object. */
  val fields: Fields

  /** Return element unique identifier. */
  var id: String = _

  def setId(value: String): this.type = {
    id = value
    this
  }

  /** Set element unique identifier. */
  // TODO remove all usages, setId will be used instead.
  def withId(value: String): this.type = {
//    def replaceSlashes(value: String) = if (value.contains("//")) value.replace("//", "/") else value
//    val idx                           = value.indexOf("://")
//    id =
//      if (idx == -1) replaceSlashes(value)
//      else {
//        val n = idx + 3
//        value.substring(0, n) + replaceSlashes(value.substring(n))
//      }

    this
  }

  /** Value , path + field value that is used to compose the id when the object its adopted */
  private[amf] def componentId: String

  /** Call after object has been adopted by specified parent. */
  // TODO remove all usages, adoptWithParentId will be used instead.
  final def simpleAdoption(parent: String): this.type = {
    withId(parent + componentId)
  }

  def adoptWithParentId(parent: String): this.type = {
    setId(parent + componentId)
  }

  /** Call after object has been adopted by specified parent. */
  // TODO remove all usages, adoptWithParentId will be used instead.
  def adopted(parent: String, cycle: Seq[String] = Seq()): this.type = simpleAdoption(parent)

  /** Set scalar value. */
  def set(field: Field, value: String): this.type = {
    set(field, AmfScalar(value))
  }

  def set(field: Field, value: String, annotations: Annotations): this.type = {
    set(field, AmfScalar(value), annotations)
  }

  /** Set scalar value. */
  def set(field: Field, value: Boolean): this.type = set(field, AmfScalar(value))

  /** Set scalar value. */
  def set(field: Field, value: Int): this.type = set(field, AmfScalar(value))

  /** Set scalar value. */
  def set(field: Field, value: Double): this.type = set(field, AmfScalar(value))

  def set(field: Field, value: Float): this.type = set(field, AmfScalar(value))

  /** Set scalar value. */
  def set(field: Field, values: Seq[String]): this.type = setArray(field, values.map(AmfScalar(_)))

  /** Set field value. */
  def set(field: Field, value: AmfElement): this.type = {
    fields.set(id, field, value)
    this
  }

  /** Add field value to array. */
  def add(field: Field, value: AmfElement): this.type = {
    fields.add(id, field, value)
    this
  }

  /** Set field value. */
  def setArray(field: Field, values: Seq[AmfElement]): this.type = {
    fields.set(id, field, AmfArray(values))
    this
  }

  /** Set field value. */
  def setArray(field: Field, values: Seq[AmfElement], annotations: Annotations): this.type = {
    fields.set(id, field, AmfArray(values), annotations)
    this
  }

  /** Set field value. */
  def setArrayWithoutId(field: Field, values: Seq[AmfElement]): this.type = {
    fields.setWithoutId(field, AmfArray(values))
    this
  }

  /** Set field value. */
  def setArrayWithoutId(field: Field, values: Seq[AmfElement], annotations: Annotations): this.type = {
    fields.setWithoutId(field, AmfArray(values), annotations)
    this
  }

  /** Set field value. */
  def set(field: Field, value: AmfElement, annotations: Annotations): this.type = {
    fields.set(id, field, value, annotations)
    this
  }

  def setWithoutId(field: Field, value: AmfElement, annotations: Annotations = Annotations()): this.type = {
    fields.setWithoutId(field, value, annotations)
    this
  }

  override def cloneElement(branch: mutable.Map[AmfObject, AmfObject]): AmfObject = {
    branch.get(this) match {
      case Some(me) if me.meta.`type`.head.iri() == meta.`type`.head.iri() => me
      case _ =>
        val obj = newInstance()
        obj.id = id
        obj.annotations ++= annotations
        branch.put(this, obj)
        fields.cloneFields(branch).into(obj.fields)
        obj
    }
  }

  private[amf] def newInstance(): AmfObject =
    meta.asInstanceOf[ModelDefaultBuilder].modelInstance // make meta be model default builder also
}
