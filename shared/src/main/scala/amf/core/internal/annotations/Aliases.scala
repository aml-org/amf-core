package amf.core.internal.annotations

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain._

case class ReferencedInfo(instanceOrId: Either[AmfObject, String], fullUrl: String, relativeUrl: String) {
  def id: String = instanceOrId match {
    case Left(instance) => instance.id
    case Right(id)      => id
  }
}
object ReferencedInfo {
  def apply(id: String, fullUrl: String, relativeUrl: String): ReferencedInfo = {
    ReferencedInfo(Right(id), fullUrl, relativeUrl)
  }
  def apply(instance: AmfObject, fullUrl: String, relativeUrl: String): ReferencedInfo = {
    ReferencedInfo(Left(instance), fullUrl, relativeUrl)
  }
}

case class Aliases(aliases: Set[(Aliases.Alias, ReferencedInfo)]) extends SerializableAnnotation with UriAnnotation {

  /** Extension name. */
  override val name: String = "aliases-array"

  /** Value as string. */
  override def value: String =
    aliases
      .map { case (alias, refInfo) => s"$alias->${refInfo.id}::${refInfo.fullUrl}::${refInfo.relativeUrl}" }
      .mkString(",")
  override def uris: Seq[String] = aliases.map(_._2.id).toSeq

  override def shorten(fn: String => String): Annotation = {
    Aliases(aliases.map {
      case (alias, refInfo) =>
        alias -> ReferencedInfo(fn(refInfo.id), refInfo.fullUrl, refInfo.relativeUrl)
    })
  }
}

object Aliases extends AnnotationGraphLoader {

  type FullUrl        = String
  type RelativeUrl    = String
  type Alias          = String
  type RefId          = String
  type ImportLocation = String

  override def unparse(annotatedValue: String, objects: Map[String, AmfElement]): Option[Annotation] =
    Some(
        Aliases(
            annotatedValue
              .split(",")
              .map(_.split("->") match {
                case Array(alias, urls) =>
                  urls.split("::") match {
                    case Array(id, fullUrl, relativeUrl) =>
                      alias -> ReferencedInfo(id, fullUrl, relativeUrl)
                  }
              })
              .toSet))
}
