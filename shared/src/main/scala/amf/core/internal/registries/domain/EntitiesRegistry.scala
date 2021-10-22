package amf.core.internal.registries.domain

import amf.core.client.scala.model.domain.AnnotationGraphLoader
import amf.core.internal.metamodel.{ModelDefaultBuilder, Obj, Type}

private[amf] case class EntitiesRegistry(domainEntities: Map[String, ModelDefaultBuilder],
                                         serializableAnnotations: Map[String, AnnotationGraphLoader],
                                         extensionTypes: Map[String, Type]) {

  def withEntities(entities: Map[String, ModelDefaultBuilder]): EntitiesRegistry =
    copy(domainEntities = domainEntities ++ entities)

  def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): EntitiesRegistry =
    copy(serializableAnnotations = serializableAnnotations ++ annotations)

  def withExtensions(extensions: Map[String, Type]): EntitiesRegistry =
    copy(extensionTypes = this.extensionTypes ++ extensions)

  private[amf] def findType(`type`: String): Option[ModelDefaultBuilder] = domainEntities.get(`type`)

  private[amf] def findAnnotation(annotationID: String): Option[AnnotationGraphLoader] =
    serializableAnnotations.get(annotationID)

  private[amf] def removeAllEntities(): EntitiesRegistry = copy(Map.empty, serializableAnnotations)

}

private[amf] object EntitiesRegistry {
  val empty: EntitiesRegistry = EntitiesRegistry(Map.empty, Map.empty, Map.empty)
}
