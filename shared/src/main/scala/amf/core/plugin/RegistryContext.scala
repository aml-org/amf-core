package amf.core.plugin

import amf.client.plugins.{AMFDocumentPlugin, AMFDomainPlugin, AMFPlugin}
import amf.client.remod.ParseConfiguration
import amf.client.remod.amfcore.registry.AMFRegistry
import amf.core.metamodel.{ModelDefaultBuilder, Obj}
import amf.core.model.domain.AmfObject
import amf.core.parser.Annotations
import amf.core.registries.AMFDomainRegistry.defaultIri
import amf.core.registries.{AMFDomainEntityResolver, AMFDomainRegistry}
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances._

import scala.collection.immutable.TreeSet

/** Context for handling plugins registration. */
case class RegistryContext private[amf] (private val amfRegistry: AMFRegistry) {

  /** Find matching type given type IRI. */
  private val findType = CachedFunction.fromMonadic { `type`: String =>
    amfRegistry.entitiesRegistry.findType(`type`)
  }

  def findType(`type`: String): Option[Obj] = findType.runCached(`type`)

  private val buildType = CachedFunction.from { `type`: Obj =>
    amfRegistry.entitiesRegistry.findType(defaultIri(`type`)) match {
      case Some(builder: ModelDefaultBuilder) =>
        (annotations: Annotations) =>
          val instance = builder.modelInstance
          instance.annotations ++= annotations
          instance
      case _ => throw new Exception(s"Cannot find builder for type ${`type`}")
    }
  }

  /** Return instance builder given type. */
  def buildType(`type`: Obj): Annotations => AmfObject = buildType.runCached(`type`)

}
