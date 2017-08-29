package amf.model

/**
  * JVM License model class.
  */
case class License private[model] (private val license: amf.domain.License) extends DomainElement {

  def this() = this(amf.domain.License())

  val url: String  = license.url
  val name: String = license.name

  override def equals(other: Any): Boolean = other match {
    case that: License =>
      (that canEqual this) &&
        license == that.license
    case _ => false
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[License]

  override private[amf] def element: amf.domain.License = license

  /** Set url property of this [[License]]. */
  def withUrl(url: String): this.type = {
    license.withUrl(url)
    this
  }

  /** Set name property of this [[License]]. */
  def withName(name: String): this.type = {
    license.withName(name)
    this
  }
}
