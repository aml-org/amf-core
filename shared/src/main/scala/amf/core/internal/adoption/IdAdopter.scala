package amf.core.internal.adoption

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfObject, NamedDomainElement}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.LinkableElementModel
import amf.core.internal.parser.domain.FieldEntry
import amf.core.internal.utils.IdCounter
import org.mulesoft.common.collections.FilterType
import scala.collection.mutable

class IdAdopter(root: AmfObject, rootId: String) {

  val visited: mutable.Map[String, AmfObject] = mutable.Map.empty
  val idGenerator                             = new IdCounter()

  def adoptFromRoot(): Unit     = adopt(isRoot = true)
  def adoptFromRelative(): Unit = adopt(isRoot = false)

  private def adopt(isRoot: Boolean): Unit = {
    adoptInnerElement(root, rootId, isRoot)
    visited.values.filterType[AdoptionDependantCalls].foreach(_.run())
  }

  private def adoptInner(field: FieldEntry, parentId: String): Unit = {
    val generatedId = makeId(parentId, relativeName(field))
    adoptInnerElement(field.element, generatedId)
  }

  private def adoptInnerElement(element: AmfElement, elementId: String, isRoot: Boolean = false): Unit = {
    element match {
      case obj: AmfObject =>
        if (notVisited(obj)) {
          val fieldOrdering = getFieldOrdering(obj)
          obj.withId(elementId)
          visited += obj.id -> obj
          while (fieldOrdering.hasPendingFields) adoptInner(fieldOrdering.nextField(),
                                                            parentId = elementId + withFragment(isRoot))
        }
      case array: AmfArray =>
        array.values.zipWithIndex.foreach {
          // TODO check to change the default 'i' with something more meaningful
          case (item, i) =>
            adoptInnerElement(item, makeId(elementId, componentId(item).getOrElse(i.toString)))
        }
      case _ => // Nothing to do
    }
  }

  private def withFragment(isRoot: Boolean) = if (isRoot) "#" else ""

  private def notVisited(obj: AmfObject): Boolean = !visited.contains(obj.id)

  private def relativeName(field: FieldEntry): String =
    componentId(field.element).getOrElse(field.field.doc.displayName)

  private def componentId(element: AmfElement): Option[String] = element match {
    case obj: AmfObject if obj.componentId.nonEmpty => Some(obj.componentId.stripPrefix("/"))
    case _                                          => None
  }

  private def makeId(parent: String, element: String): String = {
    val newId = parent + "/" + element
    if (visited.contains(newId)) idGenerator.genId(newId)
    else newId
  }

  private def getFieldOrdering(obj: AmfObject) = obj match {
    case b: BaseUnit => new BaseUnitFieldAdoptionOrdering(b)
    case other       => new GenericFieldAdoptionOrdering(other)
  }

}
