package amf.plugins.document.graph.entities

import amf.core.entities.Entities
import amf.core.metamodel.Obj
import amf.core.metamodel.domain._

private[amf] object AMFGraphEntities extends Entities {

  override protected val innerEntities: Seq[Obj] = Seq(
      ObjectNodeModel,
      ScalarNodeModel,
      ArrayNodeModel,
      LinkNodeModel,
      RecursiveShapeModel
  )

}
