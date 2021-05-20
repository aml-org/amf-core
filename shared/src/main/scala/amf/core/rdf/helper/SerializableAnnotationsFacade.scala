package amf.core.rdf.helper

import amf.client.remod.ParseConfiguration
import amf.core.model.document.SourceMap
import amf.core.model.domain.{AmfElement, AnnotationGraphLoader}
import amf.core.parser.Annotations
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances._

import scala.collection.mutable

class SerializableAnnotationsFacade private[amf] (parserConfig: ParseConfiguration) {

  def retrieveAnnotation(nodes: Map[String, AmfElement], sources: SourceMap, key: String): Annotations = {
    val result = Annotations()
    if (sources.nonEmpty) {
      sources.annotations.foreach {
        case (annotation, values: mutable.Map[String, String]) =>
          values.get(key) match {
            case Some(value) =>
              findAnnotation(annotation).flatMap(_.unparse(value, nodes)).foreach(result += _)
            case _ => // Nothing to do
          }
      }
    }
    result
  }

  private val findAnnotation = CachedFunction.fromMonadic(parserConfig.registryContext.findAnnotation)

  private def findAnnotation(annotationID: String): Option[AnnotationGraphLoader] =
    findAnnotation.runCached(annotationID)

}
