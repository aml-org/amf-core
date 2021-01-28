package amf.core.emitter

import amf.core.annotations.{LexicalInformation, SingleValueArray, SourceLocation}
import amf.core.metamodel.{Field, Type}
import amf.core.model.domain.{AmfElement, AmfObject, AmfScalar}
import amf.core.parser.Position._
import amf.core.parser.{Annotations, FieldEntry, Position, Value}
import org.yaml.model.YDocument.{EntryBuilder, PartBuilder}
import org.yaml.model._

import scala.collection.mutable

package object BaseEmitters {

  protected[amf] def pos[T <: LexicalInformation](annotations: Annotations, clazz: Class[T]): Position =
    annotations.find[LexicalInformation](clazz.isInstance(_)).map(_.range.start).getOrElse(ZERO)

  protected[amf] def pos(annotations: Annotations): Position = pos(annotations, classOf[LexicalInformation])

  protected[amf] def pos(field: Field, obj: AmfObject, default: Annotations): Position =
    obj.fields.entry(field) match {
      case Some(f) => pos(f.value.annotations)
      case None    => pos(default)
    }

  protected[amf] def traverse(emitters: Seq[EntryEmitter], b: EntryBuilder): Unit = {
    emitters.foreach(e => {
      e.emit(b)
    })
  }

  protected[amf] def traverse(emitters: Seq[PartEmitter], b: PartBuilder): Unit = {
    emitters.foreach(e => {
      e.emit(b)
    })
  }

  case class RawEmitter(content: String, tag: YType = YType.Str, annotations: Annotations = Annotations())
      extends PartEmitter {
    override def emit(b: PartBuilder): Unit = sourceOr(annotations, raw(b, content, tag))

    override def position(): Position = pos(annotations)
  }

  def raw(b: PartBuilder, content: String, tag: YType = YType.Str): Unit =
    b += YNode(YScalar(content), tag)

  case class ScalarEmitter(v: AmfScalar, tag: YType = YType.Str) extends PartEmitter {
    override def emit(b: PartBuilder): Unit =
      sourceOr(v.annotations, {
        b += YNode(YScalar(v.value), tag)
      })

    override def position(): Position = pos(v.annotations)
  }

  case class NullEmitter(annotations: Annotations) extends PartEmitter {
    override def emit(b: PartBuilder): Unit =
      b += YNode(YScalar.withLocation("null", YType.Null, annotations.sourceLocation), YType.Null)

    override def position(): Position = pos(annotations)
  }

  case class TextScalarEmitter(value: String, annotations: Annotations, tag: YType = YType.Str) extends PartEmitter {
    override def emit(b: PartBuilder): Unit = {
      sourceOr(
        annotations, {
          b += YNode(YScalar.withLocation(value, tag, annotations.sourceLocation), tag)
        }
      )

    }

    override def position(): Position = pos(annotations)
  }

  case class LinkScalaEmitter(alias: String, annotations: Annotations) extends PartEmitter {
    override def emit(b: PartBuilder): Unit =
      sourceOr(annotations, {
        b += YNode(YScalar.withLocation(alias, YType.Include, annotations.sourceLocation), YType.Include) // YNode(YScalar(alias), YType.Include)
      })

    override def position(): Position = pos(annotations)
  }

  trait BaseValueEmitter extends EntryEmitter {

    val key: String
    val f: FieldEntry

    val tag: YType = {
      f.field.`type` match {
        case Type.Date                => YType.Timestamp
        case Type.Int                 => YType.Int
        case Type.Bool                => YType.Bool
        case Type.Double | Type.Float => YType.Float
        case _                        => YType.Str
      }
    }

    override def position(): Position = pos(f.value.annotations)
  }

  case class ValueEmitter(key: String, f: FieldEntry, dataType: Option[YType] = None) extends BaseValueEmitter {

    override def emit(b: EntryBuilder): Unit = sourceOr(f.value, simpleScalar(b))

    private def simpleScalar(b: EntryBuilder): Unit = {

      val value = dataType match {
        case Some(YType.Int)   => integerValue(f.scalar.value.toString)
        case Some(YType.Float) => floatValue(f.scalar.value.toString)
        case _ => f.scalar.value
      }

      b.entry(
        key,
        YNode(YScalar(value), dataType.getOrElse(tag))
      )
    }

    private def integerValue(text: String) = BigDecimal(text).setScale(0, BigDecimal.RoundingMode.FLOOR).toString

    // Hack to fix difference in float emittion between JS and Java (Java prints '.0')
    private def floatValue(text: String) = {
      val removeLeadingZeros = BigDecimal(text).toString()
      removeLeadingZeros match {
        case e if e.endsWith(".0") => e.substring(0, e.indexOf(".0"))
        case o => o
      }
    }
  }

  object RawValueEmitter {
    def apply(key: String, f: Field, value: Any, annotations: Annotations = Annotations()) = ValueEmitter(
      key,
      FieldEntry(f, Value(AmfScalar(value, Annotations()), annotations))
    )
  }

  protected[amf] def sourceOr(value: Value, inner: => Unit): Unit = sourceOr(value.annotations, inner)

  protected[amf] def sourceOr(annotations: Annotations, inner: => Unit): Unit = {
    //    annotations
    //      .find(classOf[SourceAST])
    //      .fold(inner)(a => emitter.addChild(a.ast))
    inner
  }

  case class MapEntryEmitter(key: String, value: String, tag: YType = YType.Str, position: Position = Position.ZERO)
      extends EntryEmitter {

    override def emit(b: EntryBuilder): Unit = {
      b.entry(
        key,
        raw(_, value, tag)
      )
    }
  }

  case class EntryPartEmitter(key: String,
                              value: PartEmitter,
                              tag: YType = YType.Str,
                              position: Position = Position.ZERO)
      extends EntryEmitter {

    override def emit(b: EntryBuilder): Unit = {
      b.entry(key, value.emit _)
    }
  }

  object MapEntryEmitter {
    def apply(tuple: (String, String)): MapEntryEmitter =
      tuple match {
        case (key, value) => MapEntryEmitter(key, value)
      }
  }

  case class EmptyMapEmitter(position: Position = Position.ZERO) extends PartEmitter {

    override def emit(b: PartBuilder): Unit = b += YMap.empty
  }

  protected[amf] def link(b: PartBuilder, id: String): Unit = b.obj(_.entry("@id", id.trim))

  object ArrayEmitter {
    def apply(key: String, f: FieldEntry, ordering: SpecOrdering, forceMultiple: Boolean = false, valuesTag: YType = YType.Str) = {
      val isSingleValue = isSingleValueArray(f) || isSingleValueArray(f.element)
      if (isSingleValue && !forceMultiple) SingleValueArrayEmitter(key, f, valuesTag)
      else MultipleValuesArrayEmitter(key, f, ordering, valuesTag)
    }

    private def isSingleValueArray(element: AmfElement) = element.annotations.contains(classOf[SingleValueArray])

    private def isSingleValueArray(f: FieldEntry) = f.value.annotations.contains(classOf[SingleValueArray])
  }

  case class SingleValueArrayEmitter(key: String,
                                     f: FieldEntry,
                                     valuesTag: YType = YType.Str)
    extends EntryEmitter {

    override def emit(b: EntryBuilder): Unit = {
      sourceOr(
        f.value,
        emitSingle(b)
      )
    }

    override def position(): Position = pos(f.value.annotations)

    private def emitSingle(b: EntryBuilder): Unit = {
      val value = f.array.scalars.headOption.map(_.toString).getOrElse("")
      b.entry(key, p => raw(p, value, valuesTag))
    }
  }

  case class MultipleValuesArrayEmitter(key: String,
                                        f: FieldEntry,
                                        ordering: SpecOrdering,
                                        valuesTag: YType = YType.Str)
    extends EntryEmitter {

    override def emit(b: EntryBuilder): Unit = sourceOr(
      f.value,
     emitValues(b)
    )

    private def emitValues(b: EntryBuilder): Unit = {
      b.entry(
        key,
        b => {
          val result = mutable.ListBuffer[PartEmitter]()

          f.array.scalars
            .foreach(v => {
              result += ScalarEmitter(v, valuesTag)
            })

          b.list(b => {
            traverse(ordering.sorted(result), b)
          })
        }
      )
    }

    override def position(): Position = pos(f.value.annotations)
  }
}
