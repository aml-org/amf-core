package amf.core.client.scala.parse.document

import amf.core.client.scala.exception.CyclicReferenceException
import amf.core.client.scala.model.document.RecursiveUnit
import amf.core.client.scala.parse.{AMFParsePlugin, document}
import amf.core.internal.parser.{AMFCompiler, CompilerContext}
import amf.core.internal.remote.Spec
import amf.core.internal.unsafe.PlatformSecrets
import org.mulesoft.lexer.SourceLocation

import scala.concurrent.{ExecutionContext, Future}

case class Reference(url: String, refs: Seq[RefContainer]) extends PlatformSecrets {

  def isRemote: Boolean = !url.startsWith("#")

  def +(refContainer: ASTRefContainer): Reference = {
    copy(refs = refs :+ refContainer)
  }

  def parseReference(
      originContext: CompilerContext,
      referencePlugins: Seq[AMFParsePlugin],
      allowRecursiveRefs: Boolean)(implicit executionContext: ExecutionContext): Future[ReferenceResolutionResult] = {
    val refContext = originContext.forReference(url, referencePlugins)
    // If there is any ReferenceResolver attached to the environment, then first try to get the cached reference if it exists. If not, load and parse as usual.
    try {
      originContext.compilerConfig.getUnitsCache match {
        case Some(resolver) =>
          // cached references do not take into account allowedVendorsToReference defined in plugin
          resolver.fetch(originContext.resolvePath(url)) flatMap { cachedReference =>
            Future(ReferenceResolutionResult(None, Some(cachedReference.content)))
          } recoverWith {
            case _ => resolveReference(refContext, allowRecursiveRefs)
          }
        case None => resolveReference(refContext, allowRecursiveRefs)
      }
    } catch {
      case _: Throwable => resolveReference(refContext, allowRecursiveRefs)
    }
  }

  private def resolveReference(refContext: CompilerContext, allowRecursiveRefs: Boolean)(
      implicit executionContext: ExecutionContext): Future[ReferenceResolutionResult] = {
    val kinds = refs.map(_.linkType).distinct
    val kind  = if (kinds.size > 1) UnspecifiedReference else kinds.head
    try {
      val res: Future[Future[ReferenceResolutionResult]] = AMFCompiler.forContext(refContext, kind).build() map {
        eventualUnit =>
          Future(document.ReferenceResolutionResult(None, Some(eventualUnit)))
      } recover {
        case e: CyclicReferenceException if allowRecursiveRefs =>
          val fullUrl = e.history.last
          resolveRecursiveUnit(fullUrl, refContext).map(u => ReferenceResolutionResult(None, Some(u)))
        case e: Throwable =>
          Future(ReferenceResolutionResult(Some(e), None))
      }
      res flatMap identity
    } catch {
      case e: Throwable => Future(ReferenceResolutionResult(Some(e), None))
    }
  }

  protected def resolveRecursiveUnit(fullUrl: String, compilerContext: CompilerContext)(
      implicit executionContext: ExecutionContext): Future[RecursiveUnit] = {
    compilerContext.compilerConfig.resolveContent(fullUrl) map { content =>
      val recUnit = RecursiveUnit().adopted(fullUrl).withLocation(fullUrl)
      recUnit.withRaw(content.stream.toString)
      recUnit
    }
  }

  def isInferred: Boolean = refs.exists(_.linkType == InferredLinkReference)
}
object Reference {
  def apply(url: String, kind: ReferenceKind, pos: SourceLocation, fragment: Option[String]): Reference =
    new Reference(url, Seq(ASTRefContainer(kind, pos, fragment)))
}
