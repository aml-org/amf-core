package amf.core.client.scala.parse.document

import org.mulesoft.common.client.lexical.PositionRange
import org.yaml.model.YNode.MutRef
import org.yaml.model.{YNode, YPart, YScalar}

class SYamlRefContainer(override val linkType: ReferenceKind, val node: YPart, override val uriFragment: Option[String])
    extends ASTRefContainer(linkType, node.location, uriFragment) {

  override def reduceToLocation(): PositionRange = node match {
    case n: YNode if n.value.isInstanceOf[YScalar] =>
      val s = n.value.asInstanceOf[YScalar]
      reduceStringLength(s, uriFragment.map(l => l.length + 1).getOrElse(0), if (s.mark.plain) 0 else 1)
    case mr: MutRef => mr.origValue.range
    case _          => node.location.range
  }

  private def reduceStringLength(s: YScalar, fragmentLenght: Int, markSize: Int = 0): PositionRange = {
    val inputRange =
      if (node.location.range.columnTo < fragmentLenght && node.location.range.lineFrom < node.location.range.lineTo) {
        val lines = s.text.split('\n')
        lines.find(_.contains('#')) match {
          case Some(line) =>
            val range = node.location.range
            range
              .copy(
                  end = range.end.copy(line = node.location.range.lineFrom + lines.indexOf(line),
                                       column = line.indexOf('#') - 1))
          case _ => node.location.range
        }
      } else {
        val range = getRefValue.location.range
        range.copy(end = range.end.copy(column = node.location.range.columnTo - fragmentLenght))
      }
    PositionRange((inputRange.lineFrom, inputRange.columnFrom + markSize),
                  (inputRange.lineTo, inputRange.columnTo - markSize))
  }

  private def getRefValue: YPart = node match {
    case ref: MutRef => ref.origValue
    case _           => node
  }

}

object SYamlRefContainer {
  def apply(linkType: ReferenceKind, node: YPart, uriFragment: Option[String]) =
    new SYamlRefContainer(linkType, node, uriFragment)
}
