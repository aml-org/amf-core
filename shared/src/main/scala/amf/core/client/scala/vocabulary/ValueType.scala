package amf.core.client.scala.vocabulary

object ValueType {
  def apply(ns: Namespace, name: String) = new ValueType(ns, name)
  def apply(iri: String): ValueType =
    if (iri.contains("#")) {
      val pair = iri.split("#")
      val name = pair.last
      val ns   = pair.head + "#"
      new ValueType(Namespace(ns), name)
    } else if (iri.replace("://", "_").contains("/")) {
      val name = iri.split("/").last
      val ns   = iri.replace(name, "")
      new ValueType(Namespace(ns), name)
    } else {
      new ValueType(Namespace(iri), "")
    }
}

/** Value type. */
case class ValueType(ns: Namespace, name: String) {
  private lazy val iriValue = ns.base + name

  def iri(): String = iriValue
}
