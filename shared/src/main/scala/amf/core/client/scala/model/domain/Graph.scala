package amf.core.client.scala.model.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.parser.domain.FieldEntry
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.domain.{Annotations, FieldEntry, Value}

case class Graph(e: DomainElement) {

  private[amf] def removeField(uri: String): this.type = {
    e.fields.remove(uri)
    this
  }

  private[amf] def scalarByField(field: Field): Seq[Any] = scalarByProperty(field.value.iri())

  private[amf] def containsField(f: Field): Boolean = properties().contains(f.toString)

  private[amf] def annotationsForValue(f: Field): Annotations =
    e.fields.getValueAsOption(f).map(_.annotations).getOrElse(Annotations())

  private[amf] def patchField(patchField: Field, patchValue: Value): Unit = {
    e.set(patchField, patchValue.value, annotationsForValue(patchField))
  }

  private[amf] def patchObj(patchField: Field, mergedNode: AmfObject): Unit = {
    e.set(patchField, mergedNode, annotationsForValue(patchField))
  }

  private[amf] def patchSeqField(patchField: Field, objs: Seq[AmfElement]): Unit = {
    e.set(patchField, AmfArray(objs), annotationsForValue(patchField))
  }

  def types(): Seq[String] = e.meta.`type`.map(_.iri()).distinct

  def properties(): Seq[String] = e.fields.fields().map(_.field.value.iri()).toSeq

  def containsProperty(uri: String): Boolean = {
    properties().contains(Namespace.defaultAliases.uri(uri).iri())
  }

  def getObjectByProperty(uri: String): Seq[DomainElement] = {
    e.fields.fields().find { f: FieldEntry =>
      f.field.value.iri() == Namespace.defaultAliases.uri(uri).iri()
    } match {
      case Some(fieldEntry) =>
        fieldEntry.element match {
          case entity: DomainElement => List(entity)
          case arr: AmfArray if arr.values.nonEmpty && arr.values.head.isInstanceOf[DomainElement] =>
            arr.values.map(_.asInstanceOf[DomainElement]).toList
          case _ => List()
        }
      case None => List()
    }
  }

  def scalarByProperty(uri: String): Seq[Any] = {
    e.fields.fields().find { f: FieldEntry =>
      f.field.value.iri() == Namespace.defaultAliases.uri(uri).iri()
    } match {
      case Some(fieldEntry) =>
        fieldEntry.element match {
          case scalar: AmfScalar                    => List(scalar.value)
          case arr: AmfArray if arr.values.nonEmpty => arr.values.toList
          case _                                    => List()
        }
      case None => List()
    }
  }
}
