package amf.core.annotations.serializable

import amf.core.model.domain.AnnotationGraphLoader

private[amf] abstract class SerializableAnnotations {

  val annotations: Map[String, AnnotationGraphLoader]

}
