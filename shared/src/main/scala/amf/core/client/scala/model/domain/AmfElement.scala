package amf.core.client.scala.model.domain

import amf.core.internal.annotations.{LexicalInformation, SourceLocation, TrackedElement}
import amf.core.internal.parser.domain.Annotations

import scala.collection.mutable

/**
  * Amf element including DomainElements and BaseUnits
  */
trait AmfElement {

  /** Set of annotations for element. */
  val annotations: Annotations

  /** Add specified annotation. */
  def add(annotation: Annotation): this.type = {
    annotations += annotation
    this
  }

  /** Merge specified annotations. */
  def add(other: Annotations): this.type = {
    annotations ++= other
    this
  }

  /** search for position in annotations */
  def position(): Option[LexicalInformation] = annotations.find(classOf[LexicalInformation])

  /** search for location in annotations */
  def location(): Option[String] = annotations.find(classOf[SourceLocation]).map(_.location)

  def isTrackedBy(trackId: String): Boolean =
    annotations.collect { case t: TrackedElement if t.parents.contains(trackId) => t }.nonEmpty

  def cloneElement(branch: mutable.Map[AmfObject, AmfObject]): AmfElement
}
