package amf.core.internal.transform.stages
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.metamodel.Type.Iri
import amf.core.internal.metamodel.domain.LinkableElementModel
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain._
import amf.core.client.scala.transform.TransformationStep
import amf.core.internal.parser.domain.FieldEntry
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.domain.{Annotations, FieldEntry, Value}

import scala.collection.mutable

class UrlShortenerStage() extends TransformationStep {

  override def transform(model: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit = {
    val ids: Set[String] = Set(model.id) ++ obtainNestedReferenceIds(model)
    shorten(model, ids)
    model.withId(base)
  }

  private def obtainNestedReferenceIds[T <: BaseUnit](model: T): Seq[String] = {
    val ids = model.references.map(_.id)
    ids ++ model.references.flatMap(obtainNestedReferenceIds)
  }

  def shorten(element: AmfElement, ids: Set[String]): Unit = {
    element match {
      case o: AmfObject =>
        val shorthenId = shortener.shorten(o.id)
        if (!shorthenId.equals(o.id)) {
          o.withId(shortener.shorten(o.id))
          o.fields.fields().foreach {
            case FieldEntry(f, value: Value) if f == LinkableElementModel.Target =>
              value.value match {
                case o: AmfObject => o.withId(shortener.shorten(o.id))
                case _            => // ignore
              }
            case FieldEntry(f, value: Value) if f.`type` == Iri =>
              shorten(value.annotations)
              val v = value.value.toString
              if (ids.exists(i => v.startsWith(i)))
                value.value = AmfScalar(shortener.shorten(v), value.value.annotations)
            case FieldEntry(f, value: Value) =>
              shorten(value.value, ids)
              shorten(value.annotations)
          }
        }
      case a: AmfArray =>
        a.values.foreach { v =>
          shorten(v, ids)
        }
      case _ => // ignore
    }
    shorten(element.annotations)
  }

  private def isKnowNamespace(value: String): Boolean = {
    value
      .split("#")
      .headOption
      .flatMap(Namespace.find)
      .isEmpty
  }
  def shorten(annotations: Annotations): Unit = {
    annotations.map {
      case a: UriAnnotation => a.shorten(shortener.shorten)
      case other            => other
    }
  }

  private val shortener = Shortener()

  private case class Shortener(dictionary: mutable.Map[String, String] = mutable.Map()) {
    private var c: Int = -1

    def shorten(uri: String): String =
      if (uri.nonEmpty && !uri.startsWith(s"$base#")) {
        dictionary.getOrElseUpdate(uri, {
          c = c + 1
          s"$base#" + c
        })
      } else uri
  }

  private val base = "amf://id"

}
