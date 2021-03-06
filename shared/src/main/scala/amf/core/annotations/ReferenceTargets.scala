package amf.core.annotations

import amf.core.model.domain.Annotation
import amf.core.parser.Range

case class ReferenceTargets(targets: Map[String, Seq[Range]]) extends Annotation{
  def + (t:(String,Range)): ReferenceTargets =
    copy(targets + targets.get(t._1).map(ranges => {
      (t._1, ranges :+ t._2)
    }).getOrElse((t._1, Seq(t._2))))

  def ++ (t:Map[String,Seq[Range]]): ReferenceTargets =
    copy((targets.toSeq ++ t.toSeq).groupBy(_._1).mapValues(_.flatMap(_._2).distinct))
}