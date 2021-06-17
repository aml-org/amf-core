package amf.core.client.platform.rdf

import amf.core.client.platform.config.RenderOptions
import amf.core.client.platform.model.document.BaseUnit
import amf.core.client.scala.rdf.RdfModel
import amf.core.client.scala.rdf.{RdfUnitConverter => InternalConverter}
import amf.core.internal.convert.CoreClientConverters._

object RdfUnitConverter {

  def toNativeRdfModel(unit: BaseUnit, renderOptions: RenderOptions = new RenderOptions()): RdfModel = {
    val coreOptions = renderOptions
    InternalConverter.toNativeRdfModel(unit, coreOptions)
  }
}
