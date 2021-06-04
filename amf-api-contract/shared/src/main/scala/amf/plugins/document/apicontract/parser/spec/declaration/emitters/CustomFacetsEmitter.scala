package amf.plugins.document.apicontract.parser.spec.declaration.emitters

import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.model.document.BaseUnit
import amf.core.model.domain.extensions.PropertyShape
import amf.core.parser.FieldEntry
import amf.plugins.document.apicontract.contexts.emitter.raml.RamlSpecEmitterContext
import amf.plugins.document.apicontract.parser.spec.declaration.CustomFacetsEmitter
import amf.plugins.document.apicontract.parser.spec.declaration.emitters.raml.RamlPropertyShapeEmitter

case class RamlCustomFacetsEmitter(f: FieldEntry, ordering: SpecOrdering, references: Seq[BaseUnit])(
    implicit spec: RamlSpecEmitterContext)
    extends CustomFacetsEmitter(f, ordering, references) {

  private implicit val shapeCtx = RamlShapeEmitterContextAdapter(spec)
  override val key: String      = "facets"

  override def shapeEmitter: (PropertyShape, SpecOrdering, Seq[BaseUnit]) => EntryEmitter =
    RamlPropertyShapeEmitter.apply
}
