package amf.plugins.document.graph.entities

import amf.core.entities.Entities
import amf.core.metamodel.{ModelDefaultBuilder, Obj}
import amf.core.metamodel.domain._

private[amf] object AMFGraphEntities extends Entities {

  override protected val innerEntities: Seq[ModelDefaultBuilder] = Seq(
      ObjectNodeModel,
      ScalarNodeModel,
      ArrayNodeModel,
      LinkNodeModel
  )
}
