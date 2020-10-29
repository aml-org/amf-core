package amf.core.emitter

import org.yaml.builder.DocBuilder
import org.yaml.builder.DocBuilder.{Entry, Part, Scalar}
import org.yaml.model.{YMap, YMapEntry, YNode, YScalar, YSequence, YValue}

object YNodeDocBuilderPopulator {

  def populate[T](node: YNode, builder: DocBuilder[T]): Unit = {
    builder.doc(populatePart(node, _))
  }

  private def populatePart[T](node: YNode, part: Part[T]): Unit = {
    val value: YValue = node.value
    value match {
      case seq: YSequence =>
        part.list { partBuilder =>
          seq.nodes.foreach(populatePart(_, partBuilder))
        }
      case map: YMap =>
        part.obj { entryBuilder =>
          map.entries.foreach(populateEntry(_, entryBuilder))
        }
      case scalar: YScalar =>
        part += toScalar(scalar)

      case _ =>
    }
  }

  private def populateEntry[T](mapEntry: YMapEntry, entry: Entry[T]): Unit = {
    val key  = mapEntry.key.asOption[YScalar].map(_.text).getOrElse(mapEntry.key.toString)
    val node = mapEntry.value
    node.value match {
      case scalar: YScalar =>
        entry.entry(key, toScalar(scalar))
      case _ =>
        entry.entry(key, partBuilder => populatePart(node, partBuilder))
    }
  }

  private def toScalar(s: YScalar): Scalar = {
    s.value match {
      case b: Boolean => Scalar(b)
      case d: Double  => Scalar(d)
      case l: Long    => Scalar(l)
      case i: Integer => Scalar(i.toLong)
      case _          => Scalar(s.text)
    }
  }
}
