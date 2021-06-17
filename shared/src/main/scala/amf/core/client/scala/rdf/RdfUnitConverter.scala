package amf.core.client.scala.rdf

import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.{AMFGraphConfiguration, config}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.rdf.RdfModelParser
import amf.core.internal.unsafe.PlatformSecrets

object RdfUnitConverter extends PlatformSecrets {

  def fromNativeRdfModel(id: String, rdfModel: RdfModel, conf: AMFGraphConfiguration): BaseUnit = {
    RdfModelParser(conf).parse(rdfModel, id)
  }

  def toNativeRdfModel(unit: BaseUnit, renderOptions: RenderOptions = config.RenderOptions()): RdfModel = {
    platform.rdfFramework match {
      case Some(rdf) => rdf.unitToRdfModel(unit, renderOptions)
      case None      => throw new Exception("RDF Framework not registered cannot export to native RDF model")
    }
  }
}
