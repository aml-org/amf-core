package amf.core.internal.registries

import amf.core.client.scala.model.domain.AnnotationGraphLoader
import amf.core.internal.metamodel.ModelDefaultBuilder
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances._

/** Context for handling plugins registration. */
case class RegistryContext private[amf] (private val amfRegistry: AMFRegistry) {

  def getRegistry: AMFRegistry = amfRegistry

  /** Find matching type given type IRI. */
  def findType(`type`: String): Option[ModelDefaultBuilder] = findType.runCached(`type`)

  /** Return instance builder given type. */
  def findAnnotation(annotationID: String): Option[AnnotationGraphLoader] = findAnnotation.runCached(annotationID)

  private val findType = CachedFunction.fromMonadic { `type`: String =>
    amfRegistry.getEntitiesRegistry.findType(`type`)
  }

  private val findAnnotation = CachedFunction.fromMonadic { id: String =>
    amfRegistry.getEntitiesRegistry.findAnnotation(id)
  }

}
