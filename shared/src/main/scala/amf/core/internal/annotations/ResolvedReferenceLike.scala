package amf.core.internal.annotations

import amf.core.client.scala.model.domain._

class ResolvedReferenceLike private (override val value: String) extends EternalSerializedAnnotation {

  /** Extension name. */
  override val name: String = ResolvedReferenceLike.name

}

object ResolvedReferenceLike extends AnnotationGraphLoader {

  private val LOCAL      = "local"
  private val REMOTE     = "remote"
  private val UNRESOLVED = "unresolved"

  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] = Some(apply(value))

  def name: String                                       = "resolved-reference-like"
  def local(): ResolvedReferenceLike                     = new ResolvedReferenceLike(LOCAL)
  def remote(): ResolvedReferenceLike                    = new ResolvedReferenceLike(REMOTE)
  def unresolved(): ResolvedReferenceLike                = new ResolvedReferenceLike(UNRESOLVED)
  private def apply(kind: String): ResolvedReferenceLike = new ResolvedReferenceLike(kind)
}
