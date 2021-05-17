package amf.client.plugins

import amf.core.metamodel.Obj
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.registries.AMFDomainEntityResolver

abstract class AMFDomainPlugin extends AMFPlugin {
  // TODO - ARM: delete from here and from inherits. In new remod this is set in the configuration
  def modelEntities: Seq[Obj]
  // TODO - ARM: delete from here and from inherits. In new remod this is not used
  def modelEntitiesResolver: Option[AMFDomainEntityResolver] = None
  // TODO - ARM: delete from here and from inherits. In new remod this is set in the configuration
  def serializableAnnotations(): Map[String, AnnotationGraphLoader]
}
