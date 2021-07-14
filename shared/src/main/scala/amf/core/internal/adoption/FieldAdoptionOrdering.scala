package amf.core.internal.adoption

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.AmfObject
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.document.DocumentModel
import amf.core.internal.parser.domain.FieldEntry
import org.mulesoft.lexer.Queue

import scala.collection.mutable

abstract class FieldAdoptionOrdering(obj: AmfObject) {

  // fields to process in the added order
  protected val fieldsOrdered: mutable.LinkedHashSet[Field] = mutable.LinkedHashSet()
  private val queue: Queue[FieldEntry]                      = new Queue()

  def hasPendingFields: Boolean = !queue.isEmpty

  def nextField(): FieldEntry = queue.dequeue

  // method to define the initialization of the queue
  def init(): FieldAdoptionOrdering = {
    // first the specified fieldEntries are queued
    fieldsOrdered.foreach(enqueue)
    // then the rest of the fieldEntries
    obj.fields.fields().filterNot(f => fieldsOrdered.contains(f.field)).foreach(enqueue)
    this
  }

  private def enqueue(field: FieldEntry): Unit = queue += field
  private def enqueue(field: Field): Unit      = obj.fields.fields().find(_.field == field).foreach(enqueue)
}

// Orders the fields in 'references', 'declarations', 'encodes' and the rest of the fields
class BaseUnitFieldAdoptionOrdering(unit: BaseUnit) extends FieldAdoptionOrdering(unit) {
  fieldsOrdered.add(DocumentModel.References)
  fieldsOrdered.add(DocumentModel.Declares)
  fieldsOrdered.add(DocumentModel.Encodes)
  init()
}

// The fields are in a random order
class GenericFieldAdoptionOrdering(any: AmfObject) extends FieldAdoptionOrdering(any) {
  init()
}
