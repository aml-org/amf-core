package amf.core.internal.adoption

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfObject, AmfScalar}
import amf.core.internal.annotations.DomainExtensionAnnotation
import amf.core.internal.metamodel.Field
import amf.core.internal.utils.AmfStrings
import amf.core.internal.parser.domain.FieldEntry
import amf.core.internal.utils.IdCounter
import org.mulesoft.common.collections.FilterType
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances.{Identity, identityMonad}

import scala.collection.mutable

class IdAdopter(initialElem: AmfObject, initialId: String) {

  val adopted: mutable.Map[String, AmfObject] = mutable.Map.empty

  def adoptFromRoot(): Unit     = adopt(isRoot = true)
  def adoptFromRelative(): Unit = adopt(isRoot = false)

  private def adopt(isRoot: Boolean): Unit = {
    adoptElement(isRoot)
    adopted.values.filterType[AdoptionDependantCalls].foreach(_.run())
  }

  /**
    * adopts the initial element and all of its nested element in a BFS manner
    * @param isRoot: if the initialElement is the root base unit, used to place fragment in id.
    */
  private def adoptElement(isRoot: Boolean): Unit = {
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
    array.values.zipWithIndex.map {
      case (item, i) =>
        val generatedId = makeId(id, componentId(item).getOrElse(i.toString))
        PendingAdoption(item, generatedId)
    }
  }

  private def traverseObjFields(obj: AmfObject, isRoot: Boolean): Seq[PendingAdoption] = {
    getOrderedFields(obj).map { field =>
      val generatedId = makeId(obj.id + withFragment(isRoot), relativeName(field))
      PendingAdoption(field.element, generatedId)
    }.toList
  }

  /** this is done specifically because of RAML scalar valued nodes, extension is only stored in annotation contained in AmfScalar
    * and needs to have id defined due to potential validations
    */
  private def traverseDomainExtensionAnnotation(scalar: AmfScalar, id: String): Seq[PendingAdoption] = {
    scalar.annotations.collect[PendingAdoption] {
      case domainAnnotation: DomainExtensionAnnotation =>
        val extension   = domainAnnotation.extension
        val generatedId = makeId(id, extension.componentId)
        PendingAdoption(extension, generatedId)
    }
  }

  private def withFragment(isRoot: Boolean) = if (isRoot) "#" else ""

  private def relativeName(field: FieldEntry): String =
    componentId(field.element).getOrElse(fieldDisplayName.runCached(field.field))

  val fieldDisplayName: CachedFunction[Field, String, Identity] = CachedFunction.from[Field, String] { field =>
    field.doc.displayName.urlComponentEncoded
  }

  private def componentId(element: AmfElement): Option[String] = element match {
    case obj: AmfObject if obj.componentId.nonEmpty => Some(obj.componentId.stripPrefix("/"))
    case _                                          => None
  }

  val createdIds: mutable.Set[String] = mutable.Set.empty
  val idGenerator                     = new IdCounter()

  private def makeId(parent: String, element: String): String = {
    val newId = parent + "/" + element
    val result =
      if (createdIds.contains(newId)) idGenerator.genId(newId) // ensures no duplicate ids will be created
      else newId
    createdIds.add(newId)
    result
  }

  private def getOrderedFields(obj: AmfObject): Iterable[FieldEntry] = {
    val criteria = obj match {
      case _: BaseUnit => BaseUnitFieldAdoptionOrdering
      case _           => GenericFieldAdoptionOrdering
    }
    criteria.fields(obj)
  }
}
