package amf.core.entities

import amf.core.metamodel.{ModelDefaultBuilder, Obj}
import amf.core.metamodel.document.{DocumentModel, ExternalFragmentModel, ModuleModel, SourceMapModel}
import amf.core.metamodel.domain.{ExternalDomainElementModel, RecursiveShapeModel}
import amf.core.metamodel.domain.extensions.{
  CustomDomainPropertyModel,
  DomainExtensionModel,
  PropertyShapeModel,
  ShapeExtensionModel
}
import amf.core.metamodel.domain.templates.VariableValueModel

private[amf] object CoreEntities extends Entities {

  override protected val innerEntities: Seq[ModelDefaultBuilder] = Seq(
      DocumentModel,
      ModuleModel,
      VariableValueModel,
      RecursiveShapeModel,
      PropertyShapeModel,
      ShapeExtensionModel,
      CustomDomainPropertyModel,
      ExternalFragmentModel,
      ExternalDomainElementModel,
      DomainExtensionModel
  )

}
