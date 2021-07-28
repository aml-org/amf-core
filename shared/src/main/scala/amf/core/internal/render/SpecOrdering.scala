package amf.core.internal.render

import amf.core.internal.annotations.SourceVendor
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.remote.{Amf, Async, Oas, Raml, SpecId}
import amf.core.internal.render.emitters.Emitter

/**
  * Created by pedro.colunga on 8/22/17.
  */
object SpecOrdering {

  object Default extends SpecOrdering {
    override def sorted[T <: Emitter](values: Seq[T]): Seq[T] = values

    override def compare(x: Emitter, y: Emitter): Int = 1
  }

  object Lexical extends SpecOrdering {
    def sorted[T <: Emitter](values: Seq[T]): Seq[T] = values.partition(_.position().isZero) match {
      case (without, lexical) => lexical.sorted(this) ++ without
    }

    override def compare(x: Emitter, y: Emitter): Int = x.position().compareTo(y.position())
  }

  def ordering(target: SpecId, annotations: Annotations): SpecOrdering = {
    annotations.find(classOf[SourceVendor]) match {
      case Some(SourceVendor(source)) if source == Amf || equivalent(source, target) => Lexical
      case _                                                                         => Default
    }
  }

  private def equivalent(left: SpecId, right: SpecId) = {
    left match {
      case _: Oas   => right.isInstanceOf[Oas]
      case _: Raml  => right.isInstanceOf[Raml]
      case _: Async => right.isInstanceOf[Async]
      case _        => false
    }
  }
}

trait SpecOrdering extends Ordering[Emitter] {

  /** Return sorted values. */
  def sorted[T <: Emitter](values: Seq[T]): Seq[T]
}
