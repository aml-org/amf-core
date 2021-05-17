package amf.client.remod.amfcore.registry

import amf.core.metamodel.Obj
import amf.core.model.domain.DomainElement

private[remod] case class EntitiesRegistry(domainEntities: Map[String, Obj],
                                           wrappersRegistry: Map[String, DomainElement]) {

  private[amf] def findType(`type`: String): Option[Obj] = domainEntities.get(`type`)
}

private[remod] object EntitiesRegistry {
  val empty = EntitiesRegistry(Map.empty, Map.empty)
}
