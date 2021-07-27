package amf.core.client.scala.parse.document

import org.yaml.model.{YNode, YPart}
import org.yaml.model.YNode.MutRef

class SYamlRefContainer(override val linkType: ReferenceKind, val node: YPart, override val uriFragment: Option[String]) extends ASTRefContainer(linkType, node.location, uriFragment) {

  private def getRefValue = node match {
    case ref: MutRef => ref.origValue
    case _ => node
  }

}

object SYamlRefContainer {
  def apply(linkType: ReferenceKind, node: YPart, uriFragment: Option[String]) = new SYamlRefContainer(linkType, node, uriFragment)
}
