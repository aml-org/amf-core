package amf.core.parser

import amf.client.remod.ParseConfiguration
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.model.domain.extensions.CustomDomainProperty
import amf.core.parser.errorhandler.ParserErrorHandler
import amf.core.plugin.RegistryContext
import amf.core.validation.core.ValidationSpecification
import org.mulesoft.lexer.SourceLocation
import org.yaml.model.{IllegalTypeHandler, ParseErrorHandler, SyamlException, YError}

import scala.collection.mutable

//abstract class DataNodeParserContext(eh: ParserErrorHandler) extends ErrorHandlingContext()(eh) {
//
//  def rootContextDocument: String
//  def violation(violationId: ValidationSpecification, node: String, message: String): Unit =
//    eh.violation(violationId, node, message, rootContextDocument)
//
//}

abstract class ErrorHandlingContext(implicit val eh: ParserErrorHandler)
    extends ParseErrorHandler
    with IllegalTypeHandler {
  override def handle(location: SourceLocation, e: SyamlException): Unit = eh.handle(location, e)

  override def handle[T](error: YError, defaultValue: T): T = eh.handle(error, defaultValue)

  def violation(violationId: ValidationSpecification, node: String, message: String)
}

trait UnresolvedComponents {
  def futureDeclarations: FutureDeclarations
  def eh: ParserErrorHandler
}

object EmptyFutureDeclarations {
  def apply(): FutureDeclarations = new FutureDeclarations {}
}

case class ParserContext(rootContextDocument: String = "",
                         refs: Seq[ParsedReference] = Seq.empty,
                         futureDeclarations: FutureDeclarations = EmptyFutureDeclarations(),
                         override val eh: ParserErrorHandler)
    extends ErrorHandlingContext()(eh)
    with UnresolvedComponents
    with IllegalTypeHandler {

  var globalSpace: mutable.Map[String, Any] = mutable.Map()

  def forLocation(newLocation: String): ParserContext = {
    val copied: ParserContext = this.copy(rootContextDocument = newLocation)
    copied.globalSpace = globalSpace
    copied
  }

  private val sonsReferences: mutable.Map[String, BaseUnit] = mutable.Map()

  def addSonRef(ref: BaseUnit): this.type = this.synchronized {
    sonsReferences.get(ref.location().getOrElse(ref.id)) match {
      case Some(_) => // ignore
      case _ =>
        sonsReferences.put(ref.location().getOrElse(ref.id), ref)
    }
    this
  }

  private def getSonsParsedReferences: Seq[ParsedReference] =
    sonsReferences.values.map(u => ParsedReference(u, new Reference(u.location().getOrElse(u.id), Nil))).toSeq

  def copyWithSonsReferences(): ParserContext = {
    val context = this.copy(refs = this.refs ++ getSonsParsedReferences)
    context.globalSpace = this.globalSpace
    context
  }

  override def handle(location: SourceLocation, e: SyamlException): Unit = eh.handle(location, e)

  override def handle[T](error: YError, defaultValue: T): T = eh.handle(error, defaultValue)

  def violation(violationId: ValidationSpecification, node: String, message: String): Unit =
    eh.violation(violationId, node, message, rootContextDocument)

}
