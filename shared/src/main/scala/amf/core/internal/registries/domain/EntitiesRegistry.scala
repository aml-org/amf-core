package amf.core.internal.registries.domain

import amf.core.client.scala.model.domain.AnnotationGraphLoader
import amf.core.internal.metamodel.{ModelDefaultBuilder, Obj}

private[amf] case class EntitiesRegistry(domainEntities: Map[String, ModelDefaultBuilder],
                                         serializableAnnotations: Map[String, AnnotationGraphLoader],
                                         extensions: Map[String, Obj]) {

  def withEntities(entities: Map[String, ModelDefaultBuilder]): EntitiesRegistry =
    copy(domainEntities = domainEntities ++ entities)

  def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): EntitiesRegistry =
    copy(serializableAnnotations = serializableAnnotations ++ annotations)

  def withExtensions(extensions: Map[String, Obj]): EntitiesRegistry =
    copy(extensions = this.extensions ++ extensions)

  private[amf] def findType(`type`: String): Option[ModelDefaultBuilder] = domainEntities.get(`type`)

  private[amf] def findAnnotation(annotationID: String): Option[AnnotationGraphLoader] =
    serializableAnnotations.get(annotationID)

  private[amf] def removeAllEntities(): EntitiesRegistry = copy(Map.empty, serializableAnnotations)

}

private[amf] object EntitiesRegistry {
  val empty: EntitiesRegistry = EntitiesRegistry(Map.empty, Map.empty, Map.empty)
}
