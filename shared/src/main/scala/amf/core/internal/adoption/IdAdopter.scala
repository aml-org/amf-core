package amf.core.internal.adoption

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfObject, NamedDomainElement}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.LinkableElementModel
import amf.core.internal.parser.domain.FieldEntry

import scala.collection.mutable

class IdAdopter(root: AmfElement, rootId: String) {

  val visited: mutable.Set[String] = mutable.Set.empty

  def adopt(): Unit = {
    root match {
      case obj: AmfObject =>
        val fieldOrdering = getFieldOrdering(obj)
        obj.withId(rootId)
        visited += obj.id
        while (fieldOrdering.hasPendingFields) adoptInner(fieldOrdering.nextField(), rootId)
      case _ => // Nothing to do
    }
  }

  private def adoptInner(field: FieldEntry, parentId: String): Unit =
    if (isAllowedField(field.field)) adoptInnerElement(field.element, parentId, detectName(field))

  private def adoptInnerElement(element: AmfElement, parent: String, name: String): Unit = {
    val id = makeId(parent, name)
    element match {
      case obj: AmfObject =>
        if (notVisited(obj)) {
          val fieldOrdering = getFieldOrdering(obj)
          obj.withId(id)
          visited += obj.id
          while (fieldOrdering.hasPendingFields) adoptInner(fieldOrdering.nextField(), rootId)
        }
      case array: AmfArray =>
        array.values.zipWithIndex.foreach {
          // TODO check to change the default 'i' with something more meaningful
          case (item, i) =>
            adoptInnerElement(item, id, detectName(item).getOrElse(i.toString))
        }
      case _ => // Nothing to do
    }
  }

  private def notVisited(obj: AmfObject): Boolean = !visited.contains(obj.id)

  private def detectName(field: FieldEntry): String = detectName(field.element).getOrElse(field.field.doc.displayName)

  private def detectName(element: AmfElement): Option[String] = element match {
    case named: NamedDomainElement if named.name.nonEmpty => Some(named.name.value())
    case obj: AmfObject if obj.componentId.nonEmpty       => Some(obj.componentId)
    case _                                                => None
  }

  private def makeId(parent: String, element: String): String = parent + "/" + element

  private def getFieldOrdering(obj: AmfObject) = obj match {
    case b: BaseUnit => new BaseUnitFieldAdoptionOrdering(b).init()
    case other       => new GenericFieldAdoptionOrdering(other)
  }

  // List of fields to avoid link adoption
  private val blockedFields = Seq(
      LinkableElementModel.Target
  )

  private def isAllowedField(field: Field) = !blockedFields.contains(field)
}
