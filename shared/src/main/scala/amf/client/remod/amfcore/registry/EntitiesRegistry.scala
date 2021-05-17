package amf.client.remod.amfcore.registry

import amf.core.metamodel.Obj
import amf.core.model.domain.AnnotationGraphLoader

private[remod] case class EntitiesRegistry(domainEntities: Map[String, Obj],
                                           serializableAnnotations: Map[String, AnnotationGraphLoader]) {

  def withEntities(entities: Map[String, Obj]): EntitiesRegistry = copy(domainEntities = domainEntities ++ entities)

  def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): EntitiesRegistry =
    copy(serializableAnnotations = serializableAnnotations ++ annotations)

  private[amf] def findType(`type`: String): Option[Obj] = domainEntities.get(`type`)
}

private[remod] object EntitiesRegistry {
  val empty: EntitiesRegistry = EntitiesRegistry(Map.empty, Map.empty)
}