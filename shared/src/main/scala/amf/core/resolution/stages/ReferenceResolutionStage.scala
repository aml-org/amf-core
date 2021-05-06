package amf.core.resolution.stages

import amf.core.errorhandling.ErrorHandler
import amf.core.metamodel.document.DocumentModel
import amf.core.model.document.{BaseUnit, Document}
import amf.core.model.domain._
import amf.core.resolution.stages.elements.resolution.{
  ElementResolutionStage,
  ElementStageTransformer,
  ReferenceResolution
}
import amf.core.resolution.stages.helpers.ModelReferenceResolver
import amf.core.resolution.stages.selectors.{LinkNodeSelector, LinkSelector}

import scala.collection.mutable

class ReferenceResolutionStage(keepEditingInfo: Boolean) extends TransformationStep {
  override def transform[T <: BaseUnit](model: T, errorHandler: ErrorHandler): T = {
    new ReferenceResolutionInnerClass()(errorHandler).resolve(model)
  }

  // TODO should be in an Adapter specific for ExtendsResolution
  def resolveDomainElement[T <: DomainElement](element: T, errorHandler: ErrorHandler): T = {
    val doc = Document().withId("http://resolutionstage.com/test#")
    if (element.id != null) {
      doc.fields.setWithoutId(DocumentModel.Encodes, element)
    } else {
      doc.withEncodes(element)
    }
    transform(doc, errorHandler).encodes.asInstanceOf[T]
  }

  // TODO should be in an Adapter specific for ExtendsResolution
  def resolveDomainElementSet[T <: DomainElement](elements: Seq[T], errorHandler: ErrorHandler): Seq[DomainElement] = {
    val doc = Document().withId("http://resolutionstage.com/test#")

    doc.withDeclares(elements)
    transform(doc, errorHandler).declares
  }

  protected def customDomainElementTransformation: (DomainElement, Linkable) => DomainElement =
    (d: DomainElement, _: Linkable) => d

  private class ReferenceResolutionInnerClass(implicit val errorHandler: ErrorHandler)
      extends ElementResolutionStage[DomainElement] {

    var modelResolver: Option[ModelReferenceResolver] = None
    val cache: mutable.Map[String, DomainElement]     = mutable.Map()

    def resolve[T <: BaseUnit](model: T): T = {
      this.modelResolver = Some(new ModelReferenceResolver(model))
      model.transform(LinkSelector || LinkNodeSelector, transformation).asInstanceOf[T]
    }

    private def transformation(element: DomainElement, isCycle: Boolean): Option[DomainElement] =
      transformer.transform(element)

    override def transformer: ElementStageTransformer[DomainElement] =
      new ReferenceResolution(
          cache = cache,
          keepEditingInfo = keepEditingInfo,
          modelResolver = modelResolver,
          errorHandler = errorHandler,
          customDomainElementTransformation = customDomainElementTransformation
      )
  }
}
