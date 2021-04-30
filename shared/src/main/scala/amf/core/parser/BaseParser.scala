package amf.core.parser

import amf.core.model.domain.{AmfArray, AmfElement, AmfScalar}
import org.yaml.model._

trait TypedNode {

  type Element <: AmfElement

  /** Returns string AmfScalar of string node. */
  def string()(implicit iv: IllegalTypeHandler): Element

  /** Returns string AmfScalar of any scalar node. */
  def text()(implicit iv: IllegalTypeHandler): Element

  /** Returns integer AmfScalar of integer node. */
  def integer()(implicit iv: IllegalTypeHandler): Element

  /** Returns boolean AmfScalar of boolean node. */
  def boolean()(implicit iv: IllegalTypeHandler): Element

  def double()(implicit iv: IllegalTypeHandler): Element

  /** Returns negated boolean AmfScalar of boolean node. */
  def negated()(implicit iv: IllegalTypeHandler): Element
}

/** Scalar node. */
trait ScalarNode extends TypedNode {
  override type Element = AmfScalar
}

object ScalarNode {
  def apply(node: YNode): ScalarNode = DefaultScalarNode(node)
}

/** Array node. */
trait ArrayNode extends TypedNode {
  override type Element = AmfArray

  def obj(fn: YNode => AmfElement): AmfArray
}

object ArrayNode {
  def apply(node: YNode)(implicit iv: IllegalTypeHandler): ArrayNode = DefaultArrayNode(node)(iv)
}

/** Default scalar node. */
case class DefaultScalarNode(node: YNode) extends ScalarNode {

  override def string()(implicit iv: IllegalTypeHandler): AmfScalar  = scalar(_.as[String])
  override def text()(implicit iv: IllegalTypeHandler): AmfScalar    = scalar(_.as[YScalar].text)
  override def integer()(implicit iv: IllegalTypeHandler): AmfScalar = scalar(_.as[Int])
  override def double()(implicit iv: IllegalTypeHandler): AmfScalar  = scalar(_.as[Double])
  override def boolean()(implicit iv: IllegalTypeHandler): AmfScalar = scalar(_.as[Boolean])
  override def negated()(implicit iv: IllegalTypeHandler): AmfScalar = scalar(!_.as[Boolean])
  private def scalar(fn: YNode => Any)                               = AmfScalar(fn(node), Annotations.valueNode(node))
}

trait BaseArrayNode extends ArrayNode {

  implicit val iv: IllegalTypeHandler

  override def string()(implicit iv: IllegalTypeHandler): AmfArray  = array(scalar(_.as[String]))
  override def text()(implicit iv: IllegalTypeHandler): AmfArray    = array(scalar(_.as[YScalar].text))
  override def integer()(implicit iv: IllegalTypeHandler): AmfArray = array(scalar(_.as[Int]))
  override def double()(implicit iv: IllegalTypeHandler): AmfArray  = array(scalar(_.as[Double]))
  override def boolean()(implicit iv: IllegalTypeHandler): AmfArray = array(scalar(_.as[Boolean]))
  override def negated()(implicit iv: IllegalTypeHandler): AmfArray = array(scalar(!_.as[Boolean]))
  override def obj(fn: YNode => AmfElement): AmfArray               = array(fn)

  private def array(fn: YNode => AmfElement) = {
    nodes match {
      case (all, node) =>
        val elements = all.map(fn(_))
        AmfArray(elements, Annotations(node))
    }
  }

  /** Return all affected nodes, and node for annotation. */
  def nodes: (Seq[YNode], YNode)

  private def scalar(fn: YNode => Any)(e: YNode): AmfScalar = AmfScalar(fn(e), Annotations(e.value))
}

/** Default array node. */
case class DefaultArrayNode(node: YNode)(override implicit val iv: IllegalTypeHandler) extends BaseArrayNode {

  override def nodes: (Seq[YNode], YNode) = (node.as[Seq[YNode]], node)
}
