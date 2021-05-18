package amf.core.rdf

import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.domain.{AmfElement, Annotation, DomainElement, ExternalSourceElement}
import amf.core.parser.{EmptyFutureDeclarations, FutureDeclarations, ParsedReference, ParserContext}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class RdfParserContext(rootContextDocument: String = "",
                       refs: Seq[ParsedReference] = Seq.empty,
                       futureDeclarations: FutureDeclarations = EmptyFutureDeclarations(),
                       eh: AMFErrorHandler)
    extends ParserContext(rootContextDocument, Seq.empty, futureDeclarations, eh) {

  val unresolvedReferences       = mutable.Map[String, Seq[DomainElement]]()
  val unresolvedExtReferencesMap = mutable.Map[String, ExternalSourceElement]()

  val referencesMap = mutable.Map[String, DomainElement]()

  val collected: ListBuffer[Annotation] = ListBuffer()

  var nodes: Map[String, AmfElement] = Map()
}
