package amf.core.entities

import amf.core.metamodel.Obj

private[amf] trait Entities {

  protected val innerEntities: Seq[Obj]

  def entities: Map[String, Obj] = innerEntities.map(mapElement).toMap

  private def mapElement(element: Obj): (String, Obj) = defaultIri(element) -> element

  private def defaultIri(metadata: Obj): String = metadata.`type`.head.iri()

}
