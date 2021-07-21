package amf.core.internal.annotations

import amf.core.client.scala.model.domain._

/**
  * TrackedElement is used to register the original location of an example (parameter, payload, etc).
  * This information has to be saved as it is lost when examples are propagated to their corresponding shapes.
  */
class TrackedElement private (private val elements: Either[Set[AmfObject], Set[String]])
    extends EternalSerializedAnnotation
    with UriAnnotation {

  // defined as function because ids of objects are adopted after parsing
  def parents: Set[String] = elements match {
    case Left(instances) => instances.map(_.id)
    case Right(ids)      => ids
  }

  /** Extension name. */
  override val name: String = "tracked-element"

  /** Value as string. */
  override def value: String     = parents.mkString(",")
  override def uris: Seq[String] = parents.toSeq

  override def shorten(fn: String => String): Annotation = TrackedElement(parents.map(fn))

  def addElement(o: AmfObject): TrackedElement =
    elements match {
      case Left(instances) => TrackedElement.fromInstances(instances.filter(_.id != o.id) + o)
      case Right(ids)      => TrackedElement(ids + o.id)
    }

  def removeId(id: String): TrackedElement =
    elements match {
      case Left(instances) => TrackedElement.fromInstances(instances.filter(_.id != id))
      case Right(ids)      => TrackedElement(ids.filter(_ != id))
    }
}

object TrackedElement extends AnnotationGraphLoader {
  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] =
    Some(TrackedElement(value.split(",").toSet))

  def apply(parent: String): TrackedElement               = TrackedElement(Set(parent))
  def fromInstance(obj: AmfObject): TrackedElement        = new TrackedElement(Left(Set(obj)))
  def fromInstances(objs: Set[AmfObject]): TrackedElement = new TrackedElement(Left(objs))
  def apply(ids: Set[String]): TrackedElement             = new TrackedElement(Right(ids))
}
