package amf.core.internal.render.emitters

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.validation.CoreValidations
import org.yaml.model.{YDocument, YNode}

trait DomainElementEmitter[T] {

  /** @param emissionStructure:
    *   gives insight on the context of emission.
    * @param references:
    *   optional parameter which will improve emission of references defined in element.
    */
  def emit(element: DomainElement, emissionStructure: T, eh: AMFErrorHandler, references: Seq[BaseUnit] = Nil): YNode

  protected def nodeOrError(emitter: Option[PartEmitter], id: String, eh: AMFErrorHandler): YNode = {
    emitter
      .map { emitter =>
        YDocument(b => emitter.emit(b)).node
      }
      .getOrElse {
        eh.violation(CoreValidations.UnhandledDomainElement, id, "Unhandled domain element for given spec")
        YNode.Empty
      }
  }
}
