package amf.core.internal.adoption

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.AmfObject
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.document.DocumentModel
import amf.core.internal.parser.domain.FieldEntry
import org.mulesoft.lexer.Queue

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

trait FieldOrderingCriteria {
  def fields(obj: AmfObject): Iterable[FieldEntry]
}

object BaseUnitFieldAdoptionOrdering extends FieldOrderingCriteria {
  private val ordered = List(DocumentModel.References, DocumentModel.Declares, DocumentModel.Encodes)

  override def fields(obj: AmfObject): Iterable[FieldEntry] = {
    val result = ListBuffer[FieldEntry]()

    // first the specified fieldEntries are queued
    ordered.foreach { field =>
      obj.fields.entry(field).foreach(result += _)
    }
    // then the rest of the fieldEntries
    obj.fields.fields().filterNot(f => ordered.contains(f.field)).foreach(result += _)
    result
  }
}

// The fields are in a random order
object GenericFieldAdoptionOrdering extends FieldOrderingCriteria {
  override def fields(obj: AmfObject): Iterable[FieldEntry] = obj.fields.fields()
}
