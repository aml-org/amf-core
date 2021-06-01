package amf.client.remod.amfcore.registry

import amf.core.metamodel.{ModelDefaultBuilder, Obj}
import amf.core.model.domain.AnnotationGraphLoader

private[remod] case class EntitiesRegistry(domainEntities: Map[String, ModelDefaultBuilder],
                                           serializableAnnotations: Map[String, AnnotationGraphLoader]) {

  def withEntities(entities: Map[String, ModelDefaultBuilder]): EntitiesRegistry =
    copy(domainEntities = domainEntities ++ entities)

  def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): EntitiesRegistry =
    copy(serializableAnnotations = serializableAnnotations ++ annotations)

  private[amf] def findType(`type`: String): Option[ModelDefaultBuilder] = domainEntities.get(`type`)

  private[amf] def findAnnotation(annotationID: String): Option[AnnotationGraphLoader] =
    serializableAnnotations.get(annotationID)

}

private[remod] object EntitiesRegistry {
  val empty: EntitiesRegistry = EntitiesRegistry(Map.empty, Map.empty)
}
