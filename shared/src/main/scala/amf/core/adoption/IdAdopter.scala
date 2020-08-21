package amf.core.adoption

import amf.core.model.domain.{AmfArray, AmfElement, AmfObject, NamedDomainElement}
import amf.core.parser.FieldEntry

import scala.collection.mutable

class IdAdopter(root: AmfElement, rootId: String) {

  val visited: mutable.Set[String] = mutable.Set.empty

  def adopt(): Unit = {
    root match {
      case obj: AmfObject =>
        obj.withId(rootId)
        visited += obj.id
        obj.fields.fields().foreach(field => adoptInner(field, rootId))
      case _ => // Nothing to do
    }
  }

  private def adoptInner(field: FieldEntry, parentId: String): Unit =
    adoptInnerElement(field.element, parentId, detectName(field))

  private def adoptInnerElement(element: AmfElement, parent: String, name: String): Unit = {
    val id = makeId(parent, name)
    element match {
      case obj: AmfObject =>
        if (!visited.contains(obj.id)) {
          obj.withId(id)
          visited += obj.id
          obj.fields.fields().foreach(field => adoptInner(field, id))
        }
      case array: AmfArray =>
        array.values.zipWithIndex.foreach {
          // TODO change the default 'i' with something more identificative
          case (item, i) =>
            adoptInnerElement(item, id, detectName(item).getOrElse(i.toString))
        }
      case _ => // Nothing to do
    }
  }

  private def detectName(field: FieldEntry): String = detectName(field.element).getOrElse(field.field.doc.displayName)

  private def detectName(element: AmfElement): Option[String] = element match {
    case named: NamedDomainElement if named.name.nonEmpty => Some(named.name.value())
    case obj: AmfObject if obj.componentId.nonEmpty       => Some(obj.componentId)
    case _                                                => None
  }

  private def makeId(parent: String, element: String): String = parent + "/" + element
}
