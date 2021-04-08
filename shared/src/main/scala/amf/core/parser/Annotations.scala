package amf.core.parser

import amf.core.annotations.{Inferred, LexicalInformation, SourceAST, SourceNode, SynthesizedField, VirtualElement, SourceLocation => AmfSourceLocation}
import amf.core.model.domain.{Annotation, EternalSerializedAnnotation, SerializableAnnotation}
import org.mulesoft.lexer.{InputRange, SourceLocation}
import org.yaml.model.{YMapEntry, YNode, YPart}

import scala.collection.mutable.ListBuffer

/**
  * Element annotations
  */
class Annotations() {

  private var annotations: ListBuffer[Annotation] = new ListBuffer()

  def foreach(fn: Annotation => Unit): Unit = annotations.foreach(fn)

  def map(fn: Annotation => Annotation): Unit = annotations = annotations.map(fn)

  def find[T <: Annotation](fn: Annotation => Boolean): Option[T] = annotations.find(fn).map(_.asInstanceOf[T])

  def find[T <: Annotation](clazz: Class[T]): Option[T] = find(clazz.isInstance(_))

  def collect[T](pf: PartialFunction[Annotation, T]): Seq[T] = annotations.collect(pf)

  def contains[T <: Annotation](clazz: Class[T]): Boolean = annotations.exists(clazz.isInstance(_))

  def size: Int = annotations.size

  def sourceLocation: SourceLocation = {
    val sourceName = find(classOf[AmfSourceLocation]).map(_.location).getOrElse("")
    val range = find(classOf[LexicalInformation])
      .map(r => InputRange(r.range.start.line, r.range.start.column, r.range.end.line, r.range.end.column))
      .getOrElse(InputRange.Zero)
    SourceLocation(sourceName, range)
  }

  def +=(annotation: Annotation): this.type = {
    annotations += annotation
    this
  }

  def ++=(other: Annotations): this.type = this ++= other.annotations

  def ++=(other: TraversableOnce[Annotation]): this.type = {
    annotations ++= other
    this
  }

  def reject(p: Annotation => Boolean): this.type = {
    annotations = annotations.filter(a => !p(a))
    this
  }

  /** Return SerializableAnnotations only. */
  def serializables(): Seq[SerializableAnnotation] = collect {
    case s: SerializableAnnotation if !s.isInstanceOf[EternalSerializedAnnotation] => s
  }

  /** Return EternalSerializedAnnotations only. */
  def eternals(): Seq[EternalSerializedAnnotation] = collect { case e: EternalSerializedAnnotation => e }

  def unapply[T <: Annotation](clazz: Class[T]): Option[T] = find(clazz)

  def copy(): Annotations = Annotations(this)

  def copyFiltering(filter: Annotation => Boolean): Annotations = Annotations() ++= annotations.filter(filter)

  def into(collector: ListBuffer[Annotation], filter: Annotation => Boolean): Annotations = {
    collector ++= annotations.filter(filter)
    this
  }

  def nonEmpty: Boolean = annotations.nonEmpty
}

object Annotations {

  def apply(): Annotations = new Annotations()

  def apply(annotations: Annotations): Annotations = {
    val result = new Annotations()
    result.annotations ++= annotations.annotations
    result
  }

  def apply(ast: YPart): Annotations = {
    val annotations = new Annotations() ++= Set(LexicalInformation(ast),
                                                SourceAST(ast),
//      AmfSourceLocation(ast.sourceName))
                                                AmfSourceLocation(ast))
    ast match {
      case node: YNode      => annotations += SourceNode(node)
      case entry: YMapEntry => annotations += SourceNode(entry.value)
      case _                => annotations
    }

  }

  // todo: temp method to keep compatibility against previous range serialization logic.
  // We should discuss if always use the range of the YNode, or always use the range of the Ynode member.
  def valueNode(node: YNode): Annotations = apply(node.value) += SourceNode(node)

  def apply(annotation: Annotation): Annotations = new Annotations() += annotation

  val empty: Annotations = new Annotations() {
    override def +=(annotation: Annotation): this.type              = this
    override def ++=(other: Annotations): this.type                 = this
    override def ++=(other: TraversableOnce[Annotation]): this.type = this
  }

  def inferred(): Annotations = apply(Inferred())

  def virtual(): Annotations = apply(VirtualElement())

  def synthesized(): Annotations = apply(SynthesizedField())
}
