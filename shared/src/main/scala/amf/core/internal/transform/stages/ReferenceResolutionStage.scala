package amf.core.internal.transform.stages

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.metamodel.document.DocumentModel
import amf.core.client.scala.model.document.{BaseUnit, Document}
import amf.core.client.scala.model.domain._
import amf.core.client.scala.transform.TransformationStep
import amf.core.internal.transform.stages.elements.resolution.{ElementResolutionStage, ElementStageTransformer, ReferenceResolution}
import amf.core.internal.transform.stages.helpers.ModelReferenceResolver
import amf.core.internal.transform.stages.selectors.{LinkNodeSelector, LinkSelector}

import scala.collection.mutable

class ReferenceResolutionStage(keepEditingInfo: Boolean) extends TransformationStep {
  override def transform(model: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit = {
    new ReferenceResolutionInnerClass()(errorHandler).resolve(model)
  }

  // TODO should be in an Adapter specific for ExtendsResolution
  def resolveDomainElement[T <: DomainElement](element: T, errorHandler: AMFErrorHandler): T = {
    val doc = Document().setId("http://resolutionstage.com/test#")
    if (element.id != null) {
      doc.fields.setWithoutId(DocumentModel.Encodes, element)
    } else {
      doc.withEncodes(element)
    }
    transform(doc, errorHandler)
    doc.encodes.asInstanceOf[T]
  }

  // TODO should be in an Adapter specific for ExtendsResolution
  def resolveDomainElementSet[T <: DomainElement](elements: Seq[T],
                                                  errorHandler: AMFErrorHandler): Seq[DomainElement] = {
    val doc = Document().setId("http://resolutionstage.com/test#")

    doc.withDeclares(elements)
    transform(doc, errorHandler)
    doc.declares
  }

  protected def customDomainElementTransformation: (DomainElement, Linkable) => DomainElement =
    (d: DomainElement, _: Linkable) => d

  private class ReferenceResolutionInnerClass(implicit val errorHandler: AMFErrorHandler)
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
