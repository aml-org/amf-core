package amf.core.plugin

import amf.client.remod.amfcore.registry.AMFRegistry
import amf.core.metamodel.{ModelDefaultBuilder, Obj}
import amf.core.model.domain.{AmfObject, AnnotationGraphLoader}
import amf.core.parser.Annotations
import amf.core.registries.AMFDomainRegistry.defaultIri
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances._

/** Context for handling plugins registration. */
case class RegistryContext private[amf] (private val amfRegistry: AMFRegistry) {

  def findType(`type`: String): Option[ModelDefaultBuilder] = findType.runCached(`type`)

  /** Return instance builder given type. */
  def findAnnotation(annotationID: String): Option[AnnotationGraphLoader] = findAnnotation.runCached(annotationID)

  /** Find matching type given type IRI. */
  private val findType = CachedFunction.fromMonadic { `type`: String =>
    amfRegistry.entitiesRegistry.findType(`type`)
  }
  private val findAnnotation = CachedFunction.fromMonadic { id: String =>
    amfRegistry.entitiesRegistry.findAnnotation(id)
  }

}
