package amf.core.internal.adoption

import amf.core.client.scala.model.domain.{AmfElement, AmfObject}
import amf.core.internal.metamodel.Field
import amf.core.internal.parser.domain.FieldEntry
import amf.core.internal.utils.{AmfStrings, IdCounter}
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances.{Identity, identityMonad}

import scala.collection.mutable

trait IdMaker {
  def makeId(parent: AmfObject, field: FieldEntry, isRoot: Boolean): String
  def makeId(parent: String, element: String): String
  def makeArrayElementId(parent: String, index: Int, element: AmfElement): String
}

class DefaultIdMaker() extends IdMaker {

  private val createdIds: mutable.Set[String] = mutable.Set.empty
  private val idGenerator                     = new IdCounter()
  private val fieldDisplayName: CachedFunction[Field, String, Identity] = CachedFunction.from[Field, String] { field =>
    field.doc.displayName.urlComponentEncoded
  }
  override def makeId(parent: AmfObject, field: FieldEntry, isRoot: Boolean): String = {
    makeId(parent.id + withFragment(isRoot), relativeName(field))
  }

  override def makeId(parent: String, element: String): String = {
    val newId = parent + "/" + element
    val finalId =
      if (createdIds.contains(newId)) idGenerator.genId(newId) else newId // ensures no duplicate ids will be created
    createdIds.add(finalId)
    finalId
  }

  override def makeArrayElementId(parent: String, index: Int, element: AmfElement): String = {
    val elementIdSegment = componentId(element).getOrElse(index.toString)
    makeId(parent, elementIdSegment)
  }

  private def withFragment(isRoot: Boolean) = if (isRoot) "#" else ""

  private def relativeName(field: FieldEntry): String =
    componentId(field.element).getOrElse(fieldDisplayName.runCached(field.field))

  private def componentId(element: AmfElement): Option[String] = element match {
    case obj: AmfObject if obj.componentId.nonEmpty => Some(obj.componentId.stripPrefix("/"))
    case _                                          => None
  }
}
