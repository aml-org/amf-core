package amf.core.client.scala.traversal.iterator
import amf.core.client.scala.model.document.FieldsFilter
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfObject, DomainElement}

import scala.annotation.tailrec

case class DomainElementIterator private (
    var buffer: List[AmfElement],
    visited: VisitedCollector,
    fieldsFilter: FieldsFilter
) extends AmfIterator {

  override def hasNext: Boolean = buffer.nonEmpty

  override def next: AmfElement = {
    val current = buffer.head
    buffer = buffer.tail
    advance()
    current
  }

  @tailrec
  private def advance(): Unit = {
    if (buffer.nonEmpty) {
      val current = buffer.head
      buffer = buffer.tail
      if (visited.visited(current)) {
        advance()
      } else {
        current match {
          case obj: AmfObject =>
            advanceObject(obj)
          case arr: AmfArray =>
            buffer = arr.values.toList ++ buffer
            advance()
          case _ =>
            advance()
        }
      }
    }
  }

  private def advanceObject(obj: AmfObject): Unit = {
    val elements = fieldsFilter.filter(obj.fields)
    visited += obj
    obj match {
      case domain: DomainElement =>
        buffer = domain :: elements ++ buffer
      // advance finishes here because a non visited domain element was found
      case _ =>
        buffer = elements ++ buffer
        advance()
    }
  }
}

object DomainElementIterator {
  def apply(
      elements: List[AmfElement],
      visited: VisitedCollector = IdCollector()
  ): DomainElementIterator = {
    val iterator = new DomainElementIterator(elements, visited, FieldsFilter.All)
    iterator.advance()
    iterator
  }

  def withFilter(
      elements: List[AmfElement],
      visited: VisitedCollector = IdCollector(),
      fieldsFilter: FieldsFilter = FieldsFilter.Local
  ): DomainElementIterator = {
    val iterator = new DomainElementIterator(elements, visited, fieldsFilter)
    iterator.advance()
    iterator
  }

}
