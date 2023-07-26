package amf.core.client.scala.adoption

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfObject, AmfScalar}
import amf.core.internal.adoption.{AdoptionDependantCalls, BaseUnitFieldAdoptionOrdering, GenericFieldAdoptionOrdering}
import amf.core.internal.annotations.DomainExtensionAnnotation
import amf.core.internal.parser.domain.FieldEntry
import org.mulesoft.common.collections.FilterType

import scala.collection.mutable


class IdAdopter(
                 initialId: String,
                 private val idMaker: IdMaker = new DefaultIdMaker(),
                 private val adopted: mutable.Map[String, AmfObject] = mutable.Map.empty
               ) {

  def adoptFromRoot(initialElem: AmfObject): Unit = adopt(initialElem, isRoot = true)

  def adoptFromRelative(initialElem: AmfObject): Unit = adopt(initialElem, isRoot = false)

  private[amf] def adopt(initialElem: AmfObject, isRoot: Boolean): Unit = {
    adoptElement(initialElem, isRoot)
    adopted.values.filterType[AdoptionDependantCalls].foreach(_.run())
  }

  /** adopts the initial element and all of its nested element in a BFS manner
   *
   * @param isRoot :
   *               if the initialElement is the root base unit, used to place fragment in id.
   */
  private def adoptElement(initialElem: AmfObject, isRoot: Boolean): Unit = {
    val adoptionQueue: mutable.Queue[PendingAdoption] = new mutable.Queue()
    adoptionQueue.enqueue(PendingAdoption(initialElem, initialId, isRoot))
    adoptQueue(adoptionQueue)
  }

  private case class PendingAdoption(element: AmfElement, elementId: String, isRoot: Boolean = false)

  private def adoptQueue(queue: mutable.Queue[PendingAdoption]): Unit = {
    while (queue.nonEmpty) {
      val dequeued = queue.dequeue
      dequeued.element match {
        case obj: AmfObject =>
          if (!adopted.contains(obj.id)) {
            obj.withId(dequeued.elementId)
            adopted += obj.id -> obj
            traverseObjFields(obj, dequeued.isRoot).foreach(queue.enqueue(_))
          }
        case array: AmfArray =>
          traverseArrayValues(array, dequeued.elementId).foreach(queue.enqueue(_))
        case scalar: AmfScalar if scalar.annotations.contains(classOf[DomainExtensionAnnotation]) =>
          traverseDomainExtensionAnnotation(scalar, dequeued.elementId).foreach(queue.enqueue(_))
        case _ => // Nothing to do
      }
    }
  }

  private def traverseArrayValues(array: AmfArray, id: String): Seq[PendingAdoption] = {
    array.values.zipWithIndex.map { case (item, i) =>
      val generatedId = idMaker.makeArrayElementId(id, i, item)
      PendingAdoption(item, generatedId)
    }
  }

  private def traverseObjFields(obj: AmfObject, isRoot: Boolean): Seq[PendingAdoption] = {
    getOrderedFields(obj).map { field =>
      val generatedId = idMaker.makeId(obj, field, isRoot)
      PendingAdoption(field.element, generatedId)
    }.toList
  }

  /** this is done specifically because of RAML scalar valued nodes, extension is only stored in annotation contained in
   * AmfScalar and needs to have id defined due to potential validations
   */
  private def traverseDomainExtensionAnnotation(scalar: AmfScalar, id: String): Seq[PendingAdoption] = {
    scalar.annotations.collect[PendingAdoption] { case domainAnnotation: DomainExtensionAnnotation =>
      val extension = domainAnnotation.extension
      val generatedId = idMaker.makeId(id, extension.componentId)
      PendingAdoption(extension, generatedId)
    }
  }

  private def getOrderedFields(obj: AmfObject): Iterable[FieldEntry] = {
    val criteria = obj match {
      case _: BaseUnit => BaseUnitFieldAdoptionOrdering
      case _ => GenericFieldAdoptionOrdering
    }
    criteria.fields(obj)
  }
}
