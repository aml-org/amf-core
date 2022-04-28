package amf.core.internal.annotations

import amf.core.client.scala.model.domain.Annotation
import org.mulesoft.common.client.lexical.PositionRange

case class ReferenceTargets(targets: Map[String, Seq[PositionRange]]) extends Annotation {
  def +(t: (String, PositionRange)): ReferenceTargets =
    copy(
        targets + targets
          .get(t._1)
          .map(ranges => {
            (t._1, ranges :+ t._2)
          })
          .getOrElse((t._1, Seq(t._2)))
    )

  def ++(t: Map[String, Seq[PositionRange]]): ReferenceTargets =
    copy((targets.toSeq ++ t.toSeq).groupBy(_._1).mapValues(_.flatMap(_._2).distinct))
}
