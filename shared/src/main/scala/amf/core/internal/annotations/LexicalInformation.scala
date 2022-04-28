package amf.core.internal.annotations

import amf.core.client.scala.model.domain._
import org.mulesoft.common.client.lexical.{Position, PositionRange}
import org.yaml.model.YNode.MutRef
import org.yaml.model.YPart

case class LexicalInformation(range: PositionRange) extends SerializableAnnotation with PerpetualAnnotation {
  override val name: String = "lexical"

  override val value: String = range.toString
}

object LexicalInformation extends AnnotationGraphLoader {
  override def unparse(annotatedValue: String, objects: Map[String, AmfElement]): Option[Annotation] =
    Some(LexicalInformation.apply(annotatedValue))

  def apply(range: String): LexicalInformation = new LexicalInformation(PositionRange.apply(range))
  def apply(lineFrom: Int, columnFrom: Int, lineTo: Int, columnTo: Int) =
    new LexicalInformation(PositionRange((lineFrom, columnFrom), (lineTo, columnTo)))
  def apply(startPosition: Position, endPosition: Position) =
    new LexicalInformation(PositionRange(startPosition, endPosition))

  def apply(ast: YPart): LexicalInformation = {
    val range = ast match {
      case m: MutRef =>
        m.target.map(_.range).getOrElse(m.range)
      case _ => ast.range
    }
    new LexicalInformation(range)
  }
}

class HostLexicalInformation(override val range: PositionRange) extends LexicalInformation(range) {
  override val name = "host-lexical"
}

object HostLexicalInformation extends AnnotationGraphLoader {
  override def unparse(annotatedValue: String, objects: Map[String, AmfElement]): Option[Annotation] =
    Some(HostLexicalInformation.apply(PositionRange(annotatedValue)))

  def apply(range: PositionRange): HostLexicalInformation = new HostLexicalInformation(range)
}

class BasePathLexicalInformation(override val range: PositionRange) extends LexicalInformation(range) {
  override val name = "base-path-lexical"
}

object BasePathLexicalInformation extends AnnotationGraphLoader {
  override def unparse(annotatedValue: String, objects: Map[String, AmfElement]): Option[Annotation] =
    Some(BasePathLexicalInformation(PositionRange(annotatedValue)))

  def apply(range: PositionRange): BasePathLexicalInformation = new BasePathLexicalInformation(range)
}
