package amf.core.entities

import amf.core.metamodel.ModelDefaultBuilder

private[amf] trait Entities {

  protected val innerEntities: Seq[ModelDefaultBuilder]

  def entities: Map[String, ModelDefaultBuilder] = innerEntities.map(mapElement).toMap

  private def mapElement(element: ModelDefaultBuilder): (String, ModelDefaultBuilder) = defaultIri(element) -> element

  private def defaultIri(metadata: ModelDefaultBuilder): String = metadata.`type`.head.iri()

}
