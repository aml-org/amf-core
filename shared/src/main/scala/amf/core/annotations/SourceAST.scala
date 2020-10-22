package amf.core.annotations

import amf.core.model.domain.{Annotation, PerpetualAnnotation}
import org.yaml.model.YNode.MutRef
import org.yaml.model.{YNode, YPart}

case class SourceAST(ast: YPart) extends Annotation

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
