package amf.core.internal.parser

import amf.core.client.common.remote.Content
import amf.core.client.scala.config._
import amf.core.client.scala.exception.{CyclicReferenceException, UnsupportedMediaTypeException}
import amf.core.client.scala.model.document.{BaseUnit, ExternalFragment}
import amf.core.client.scala.model.domain.ExternalDomainElement
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.TaggedReferences._
import amf.core.client.scala.parse.document.{UnresolvedReference => _, _}
import amf.core.internal.remote.Mimes._
import amf.core.internal.remote._
import amf.core.internal.utils.AmfStrings
import amf.core.internal.validation.CoreValidations._
import amf.core.internal.validation.core.ValidationSpecification
import org.yaml.model.YPart

import java.net.URISyntaxException
import scala.concurrent.Future.failed
import scala.concurrent.{ExecutionContext, Future}

class AMFCompiler(compilerContext: CompilerContext, val referenceKind: ReferenceKind = UnspecifiedReference) {

  private def notifyEvent(e: AMFEvent): Unit = compilerContext.compilerConfig.notifyEvent(e)

  def build()(implicit executionContext: ExecutionContext): Future[BaseUnit] = {
    if (compilerContext.hasCycles) failed(new CyclicReferenceException(compilerContext.fileContext.history))
    else compilerContext.runInCache(() => compile())
  }

  private def compile()(implicit executionContext: ExecutionContext): Future[BaseUnit] = {
    notifyEvent(StartingParsingEvent(compilerContext.path))
    for {
      content <- fetchContent()
      ast     <- Future.successful(parseSyntax(content))
      parsed  <- parseDomain(ast)
    } yield {
      notifyEvent(FinishedParsingEvent(compilerContext.path, parsed))
      parsed
    }
  }

  private def autodetectSyntax(location: String, stream: CharSequence): Option[String] = {
    if (stream.length() > 2 && stream.charAt(0) == '#' && stream.charAt(1) == '%') {
      notifyEvent(DetectedSyntaxMediaTypeEvent(location, `application/yaml`))
      Some(`application/yaml`)
    } else {
      compilerContext.platform.findCharInCharSequence(stream) { c =>
        c != '\n' && c != '\t' && c != '\r' && c != ' '
      } match {
        case Some(c) if c == '{' || c == '[' =>
          notifyEvent(DetectedSyntaxMediaTypeEvent(location, `application/json`))
          Some(`application/json`)
        case _ => None
      }
    }
  }

  private[amf] def parseSyntax(input: Content): Either[Content, Root] = {
    notifyEvent(StartingContentParsingEvent(compilerContext.path, input))
    val parsed: Option[(String, ParsedDocument)] =
      input.mime
        .flatMap(mime => parseSyntaxForMediaType(input, mime))
        .orElse {
          inferMediaTypeFromFileExtension(input).flatMap(inferred => parseSyntaxForMediaType(input, inferred))
        }
        .orElse {
          autodetectSyntax(compilerContext.path, input.stream).flatMap(inferred =>
            parseSyntaxForMediaType(input, inferred))
        }

    parsed match {
      case Some((effective, document)) =>
        notifyEvent(ParsedSyntaxEvent(compilerContext.path, input, document))
        Right(Root(document, input.url, effective, Seq(), referenceKind, input.stream.toString))
      case None =>
        Left(input)
    }
  }

  private def inferMediaTypeFromFileExtension(content: Content): Option[String] = {
    FileMediaType
      .extension(content.url)
      .flatMap(FileMediaType.mimeFromExtension)
  }

  private def parseSyntaxForMediaType(content: Content, mime: String): Option[(String, ParsedDocument)] = {
    val withContentUrl = compilerContext.parserContext.forLocation(content.url)
    compilerContext.compilerConfig.sortedParseSyntax
      .find(_.applies(content.stream))
      .map(p => (mime, p.parse(content.stream, mime, withContentUrl)))
  }

  def parseExternalFragment(content: Content)(implicit executionContext: ExecutionContext): Future[BaseUnit] = Future {
    val result = ExternalDomainElement().withId(content.url + "#/").withRaw(content.stream.toString)
    content.mime.foreach(mime => result.withMediaType(mime))
    ExternalFragment()
      .withLocation(content.url)
      .withId(content.url)
      .withEncodes(result)
      .withLocation(content.url)
  }

  private def parseDomain(parsed: Either[Content, Root])(
      implicit executionContext: ExecutionContext): Future[BaseUnit] = {
    parsed match {
      case Left(content)   => parseExternalFragment(content)
      case Right(document) => parseDomain(document)
    }
  }

  private def isRoot = compilerContext.fileContext.history.length == 1

  private def parseDomain(document: Root)(implicit executionContext: ExecutionContext): Future[BaseUnit] = {
    val domainPluginOption = getDomainPluginFor(document)
    val futureDocument: Future[BaseUnit] = domainPluginOption match {
      case Some(domainPlugin) =>
        notifyEvent(SelectedParsePluginEvent(document.location, domainPlugin))
        parseReferences(document, domainPlugin) map { documentWithReferences =>
          val baseUnit =
            domainPlugin.parse(documentWithReferences, compilerContext.parserContext.copyWithSonsReferences())
          if (document.location == compilerContext.fileContext.root) baseUnit.withRoot(true)
          baseUnit.withRaw(document.raw).tagReferences(documentWithReferences)
        }
      case None =>
        Future.successful { compilerContext.compilerConfig.chooseFallback(document) }
    }
    futureDocument map { unit =>
      // we setup the run for the parsed unit
      parsedModelEvent(unit)
      unit
    }
  }

  private def parsedModelEvent(baseUnit: BaseUnit): Unit = {
    notifyEvent(ParsedModelEvent(compilerContext.path, baseUnit))
  }

  private[amf] def getDomainPluginFor(document: Root): Option[AMFParsePlugin] = {
    val allowed =
      if (isRoot) compilerContext.compilerConfig.sortedParsePlugins
      else {
        filterByAllowed(compilerContext.compilerConfig.sortedParsePlugins, compilerContext.allowedSpecs)
      }
    allowed.find(_.applies(document))
  }

  /**
    * filters plugins that are allowed given the current compiler context.
    */
  private def filterByAllowed(plugins: Seq[AMFParsePlugin], allowed: Seq[Spec]): Seq[AMFParsePlugin] =
    if (allowed.nonEmpty) plugins.filter(p => allowed.contains(p.spec))
    else plugins

  private[amf] def parseReferences(root: Root, domainPlugin: AMFParsePlugin)(
      implicit executionContext: ExecutionContext): Future[Root] = {
    val handler      = domainPlugin.referenceHandler(compilerContext.compilerConfig.eh)
    val allowedSpecs = domainPlugin.validSpecsToReference :+ domainPlugin.spec
    val refs         = handler.collect(root.parsed, compilerContext.parserContext)
    notifyEvent(FoundReferencesEvent(root.location, refs.toReferences.size))
    val parsed: Seq[Future[Option[ParsedReference]]] = refs.toReferences
      .filter(_.isRemote)
      .map { link =>
        val nodes = link.refs.map(_.node)
        link.resolve(compilerContext, allowedSpecs, domainPlugin.allowRecursiveReferences) flatMap {
          case ReferenceResolutionResult(_, Some(unit)) =>
            val reference = ParsedReference(unit, link)
            handler.update(reference, compilerContext).map(Some(_))
          case ReferenceResolutionResult(Some(e), _) =>
            e match {
              case e: CyclicReferenceException if !domainPlugin.allowRecursiveReferences =>
                compilerContext.violation(CycleReferenceError, link.url, e.getMessage, link.refs.head.node)
                Future(None)
              case _ =>
                if (!link.isInferred) {
                  nodes.foreach(compilerContext.violation(UnresolvedReference, link.url, e.getMessage, _))
                }
                Future(None)
            }
          case _ => Future(None)
        }
      }

    Future.sequence(parsed).map(rs => root.copy(references = rs.flatten))
  }

  private[amf] def fetchContent()(implicit executionContext: ExecutionContext): Future[Content] =
    compilerContext.fetchContent()

  def root()(implicit executionContext: ExecutionContext): Future[Root] = fetchContent().map(parseSyntax).flatMap {
    case Right(document: Root) =>
      val parsePlugin = compilerContext.compilerConfig.sortedParsePlugins.find(_.applies(document))
      parsePlugin match {
        case Some(domainPlugin) =>
          parseReferences(document, domainPlugin)
        case None =>
          Future {
            document
          }
      }
    case Left(content) =>
      throw new Exception(s"Cannot parse document with mime type ${content.mime.getOrElse("none")}")
  }

}

object AMFCompiler {

  // interface used by amf-service
  def apply(url: String,
            base: Context,
            cache: Cache,
            parserConfig: CompilerConfiguration,
            referenceKind: ReferenceKind = UnspecifiedReference): AMFCompiler = {
    val context = new CompilerContextBuilder(url, base.platform, parserConfig)
      .withCache(cache)
      .withFileContext(base)
      .build()
    forContext(context, referenceKind)
  }

  // could not add new environment in this method as it forces breaking changes in ReferenceHandler
  def forContext(compilerContext: CompilerContext, referenceKind: ReferenceKind = UnspecifiedReference): AMFCompiler = {
    new AMFCompiler(compilerContext, referenceKind)
  }
}
