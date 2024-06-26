package amf.core.client.scala.parse.document

import amf.core.client.scala.config.UnitCacheHitEvent
import amf.core.client.scala.exception.CyclicReferenceException
import amf.core.client.scala.model.document.RecursiveUnit
import amf.core.client.scala.parse.document
import amf.core.internal.parser.{AMFCompiler, CompilerContext}
import amf.core.internal.remote.Spec
import amf.core.internal.unsafe.PlatformSecrets
import org.mulesoft.common.client.lexical.SourceLocation

import scala.concurrent.{ExecutionContext, Future}

case class Reference(url: String, refs: Seq[RefContainer]) extends PlatformSecrets {

  def isRemote: Boolean = !url.startsWith("#")

  def +(refContainer: ASTRefContainer): Reference = {
    copy(refs = refs :+ refContainer)
  }

  def resolve(compilerContext: CompilerContext, allowedSpecs: Seq[Spec], allowRecursiveRefs: Boolean)(implicit
      executionContext: ExecutionContext
  ): Future[ReferenceResolutionResult] = {
    // If there is any ReferenceResolver attached to the environment, then first try to get the cached reference if it exists. If not, load and parse as usual.
    try {
      compilerContext.compilerConfig.getUnitsCache match {
        case Some(resolver) =>
          // cached references do not take into account allowedVendorsToReference defined in plugin
          val processedPath = compilerContext.resolvePath(url)
          resolver.fetch(processedPath) flatMap { cachedReference =>
            compilerContext.compilerConfig.notifyEvent(UnitCacheHitEvent(url, processedPath))
            Future(ReferenceResolutionResult(None, Some(cachedReference.content)))
          } recoverWith { case _ =>
            resolveReference(compilerContext, allowedSpecs, allowRecursiveRefs)
          }
        case None => resolveReference(compilerContext, allowedSpecs, allowRecursiveRefs)
      }
    } catch {
      case _: Throwable => resolveReference(compilerContext, allowedSpecs, allowRecursiveRefs)
    }
  }

  private def resolveReference(compilerContext: CompilerContext, allowedSpecs: Seq[Spec], allowRecursiveRefs: Boolean)(
      implicit executionContext: ExecutionContext
  ): Future[ReferenceResolutionResult] = {
    val kinds = refs.map(_.linkType).distinct
    val kind  = if (kinds.size > 1) UnspecifiedReference else kinds.head
    try {
      val context = compilerContext.forReference(url, allowedSpecs = allowedSpecs)

      AMFCompiler.forContext(context, kind).build() flatMap { eventualUnit =>
        Future.successful(document.ReferenceResolutionResult(None, Some(eventualUnit)))
      } recoverWith {
        case e: CyclicReferenceException if allowRecursiveRefs =>
          val fullUrl = e.history.last
          resolveRecursiveUnit(fullUrl, compilerContext).map(u => ReferenceResolutionResult(None, Some(u)))
        case e: Throwable =>
          Future.successful(ReferenceResolutionResult(Some(e), None))
      }
    } catch {
      case e: Throwable => Future.successful(ReferenceResolutionResult(Some(e), None))
    }
  }

  protected def resolveRecursiveUnit(fullUrl: String, compilerContext: CompilerContext)(implicit
      executionContext: ExecutionContext
  ): Future[RecursiveUnit] = {
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
