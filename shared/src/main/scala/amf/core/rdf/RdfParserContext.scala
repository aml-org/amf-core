package amf.core.rdf

import amf.client.remod.ParseConfiguration
import amf.core.model.domain.{AmfElement, Annotation, DomainElement, ExternalSourceElement}
import amf.core.parser.{EmptyFutureDeclarations, FutureDeclarations, ParserContext}
import amf.core.rdf.helper.SerializableAnnotationsFacade

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class RdfParserContext(rootContextDocument: String = "",
                       futureDeclarations: FutureDeclarations = EmptyFutureDeclarations(),
                       config: ParseConfiguration)
    extends ParserContext(rootContextDocument, Seq.empty, futureDeclarations, config) {

  val unresolvedReferences: mutable.Map[String, Seq[DomainElement]] = mutable.Map[String, Seq[DomainElement]]()
  val unresolvedExtReferencesMap: mutable.Map[String, ExternalSourceElement] =
    mutable.Map[String, ExternalSourceElement]()

  val referencesMap: mutable.Map[String, DomainElement] = mutable.Map[String, DomainElement]()

  val collected: ListBuffer[Annotation] = ListBuffer()

  var nodes: Map[String, AmfElement] = Map()

  val annotationsFacade: SerializableAnnotationsFacade = config.serializableAnnotationsFacade
}
