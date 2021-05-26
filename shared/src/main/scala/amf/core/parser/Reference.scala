package amf.core.parser

import amf.core.benchmark.ExecutionLog
import amf.core.exception.CyclicReferenceException
import amf.core.model.document.RecursiveUnit
import amf.core.unsafe.PlatformSecrets
import amf.core.{AMFCompiler, CompilerContext, parser}
import org.yaml.model.YNode

import scala.concurrent.{ExecutionContext, Future}

case class Reference(url: String, refs: Seq[RefContainer]) extends PlatformSecrets {

  def isRemote: Boolean = !url.startsWith("#")

  def +(kind: ReferenceKind, ast: YNode, fragment: Option[String]): Reference = {
    copy(refs = refs :+ RefContainer(kind, ast, fragment))
  }

  def resolve(compilerContext: CompilerContext, allowedMediaTypes: Seq[String], allowRecursiveRefs: Boolean)(
      implicit executionContext: ExecutionContext): Future[ReferenceResolutionResult] = {
    // If there is any ReferenceResolver attached to the environment, then first try to get the cached reference if it exists. If not, load and parse as usual.
    try {
      compilerContext.parserContext.config.getUnitsCache match {
        case Some(resolver) =>
          // cached references do not take into account allowedVendorsToReference defined in plugin
          resolver.fetch(compilerContext.resolvePath(url)) flatMap { cachedReference =>
            Future(ReferenceResolutionResult(None, Some(cachedReference.content)))
          } recoverWith {
            case _ => resolveReference(compilerContext, allowedMediaTypes, allowRecursiveRefs)
          }
        case None => resolveReference(compilerContext, allowedMediaTypes, allowRecursiveRefs)
      }
    } catch {
      case _: Throwable => resolveReference(compilerContext, allowedMediaTypes, allowRecursiveRefs)
    }
  }

  private def resolveReference(
      compilerContext: CompilerContext,
      allowedMediaTypes: Seq[String],
      allowRecursiveRefs: Boolean)(implicit executionContext: ExecutionContext): Future[ReferenceResolutionResult] = {
    val kinds = refs.map(_.linkType).distinct
    val kind  = if (kinds.size > 1) UnspecifiedReference else kinds.head
    try {
      val context = compilerContext.forReference(url, allowedMediaTypes = Some(allowedMediaTypes))
      val res: Future[Future[ReferenceResolutionResult]] = AMFCompiler.forContext(context, None, kind).build() map {
        eventualUnit =>
          Future(parser.ReferenceResolutionResult(None, Some(eventualUnit)))
      } recover {
        case e: CyclicReferenceException if allowRecursiveRefs =>
          val fullUrl = e.history.last
          resolveRecursiveUnit(fullUrl, compilerContext).map(u => ReferenceResolutionResult(None, Some(u)))
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
    ExecutionLog.log(s"AMFCompiler#parserReferences: Recursive reference $fullUrl")
    compilerContext.parserContext.config.resolveContent(fullUrl) map { content =>
      val recUnit = RecursiveUnit().adopted(fullUrl).withLocation(fullUrl)
      recUnit.withRaw(content.stream.toString)
      recUnit
    }
  }

  def isInferred: Boolean = refs.exists(_.linkType == InferredLinkReference)
}
object Reference {
  def apply(url: String, kind: ReferenceKind, node: YNode, fragment: Option[String]): Reference =
    new Reference(url, Seq(RefContainer(kind, node, fragment)))
}
