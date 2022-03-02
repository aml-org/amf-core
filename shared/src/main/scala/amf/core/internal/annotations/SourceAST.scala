package amf.core.internal.annotations

import amf.core.client.scala.model.domain.{Annotation, PerpetualAnnotation}
import org.yaml.model.YNode.MutRef
import org.yaml.model.{YNode, YPart}

trait SourceAST[T] extends Annotation {
  val ast: T
}

case class SourceYPart(override val ast: YPart) extends SourceAST[YPart]

case class SourceNode(node: YNode) extends Annotation

case class SourceLocation(location: String) extends PerpetualAnnotation

object SourceLocation {
  def apply(ast: YPart): SourceLocation = {
    val location = ast match {
      case m: MutRef =>
        m.target.map(_.sourceName).getOrElse(m.sourceName)
      case _ => ast.sourceName
    }
    new SourceLocation(location)
  }
}
