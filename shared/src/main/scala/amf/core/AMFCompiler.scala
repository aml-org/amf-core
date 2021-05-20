package amf.core

import amf.client.remod.ParseConfiguration
import amf.client.remod.amfcore.config._
import amf.client.remod.amfcore.plugins.parse.AMFParsePlugin
import amf.client.remote.Content
import amf.core.TaggedReferences._
import amf.core.benchmark.ExecutionLog
import amf.core.exception.{CyclicReferenceException, UnsupportedMediaTypeException}
import amf.core.model.document.{BaseUnit, ExternalFragment}
import amf.core.model.domain.ExternalDomainElement
import amf.core.parser.{
  ParsedDocument,
  ParsedReference,
  ParserContext,
  ReferenceKind,
  ReferenceResolutionResult,
  UnspecifiedReference
}
import amf.core.remote._
import amf.core.services.RuntimeCompiler
import amf.core.utils.AmfStrings
import amf.core.validation.core.ValidationSpecification
import amf.plugins.features.validation.CoreValidations._
import org.yaml.model.YPart

import java.net.URISyntaxException
import scala.concurrent.Future.failed
import scala.concurrent.{ExecutionContext, Future}

object AMFCompilerRunCount {
  val NONE: Int = -1
  var count     = 0

  def nextRun(): Int = synchronized {
    count += 1
    count
  }
}

class CompilerContext(val url: String,
                      val parserContext: ParserContext,
                      val fileContext: Context,
                      val allowedMediaTypes: Option[Seq[String]],
                      cache: Cache) {

  implicit val executionContext: ExecutionContext = parserContext.config.executionContext

  /**
    * The resolved path that result to be the normalized url
    */
  val location: String = fileContext.current
  val path: String     = url.normalizePath

  def runInCache(fn: () => Future[BaseUnit]): Future[BaseUnit] = cache.getOrUpdate(location, fileContext)(fn)

  def logForFile(message: String): Unit = ExecutionLog.log(message + s" in ${path}")

  lazy val hasCycles: Boolean = fileContext.hasCycles

  lazy val platform: Platform = fileContext.platform

  def resolvePath(url: String): String = fileContext.resolve(fileContext.platform.normalizePath(url))

  def fetchContent(): Future[Content] = parserContext.config.resolveContent(location)

  def forReference(refUrl: String, allowedMediaTypes: Option[Seq[String]] = None)(
      implicit executionContext: ExecutionContext): CompilerContext = {

    val builder = new CompilerContextBuilder(refUrl, fileContext.platform, parserContext.config)
      .withFileContext(fileContext)
      .withBaseParserContext(parserContext)
      .withCache(cache)

    allowedMediaTypes.foreach(builder.withAllowedMediaTypes)
    builder.build()
  }

  def violation(id: ValidationSpecification, node: String, message: String, ast: YPart): Unit =
    parserContext.eh.violation(id, node, message, ast)

  def violation(id: ValidationSpecification, message: String, ast: YPart): Unit = violation(id, "", message, ast)
}

class CompilerContextBuilder(url: String, platform: Platform, config: ParseConfiguration) {

  private var fileContext: Context                   = Context(platform)
  private var cache                                  = Cache()
  private var givenContent: Option[ParserContext]    = None
  private var allowedMediaTypes: Option[Seq[String]] = None

  def withFileContext(fc: Context): CompilerContextBuilder = {
    fileContext = fc
    this
  }

  def withCache(cache: Cache): CompilerContextBuilder = {
    this.cache = cache
    this
  }

  def withAllowedMediaTypes(allowed: Seq[String]): CompilerContextBuilder = {
    this.allowedMediaTypes = Some(allowed)
    this
  }

  def withBaseParserContext(parserContext: ParserContext): this.type = {
    givenContent = Some(parserContext)
    this
  }

  /**
    * normalized url
    * */
  private def path: String = {
    try {
      url.normalizePath
    } catch {
      case e: URISyntaxException =>
        config.eh.violation(UriSyntaxError, url, e.getMessage)
        url
      case e: Exception => throw new PathResolutionError(e.getMessage)
    }
  }

  private def buildFileContext() = fileContext.update(path)

  private def buildParserContext(fc: Context) = givenContent match {
    case Some(given) => given.forLocation(fc.current)
    case None        => ParserContext(fc.current, config = config)
  }

  def build(): CompilerContext = {
    val fc = buildFileContext()
    new CompilerContext(url, buildParserContext(fc), fc, allowedMediaTypes, cache)
  }
}

class AMFCompiler(compilerContext: CompilerContext,
                  val mediaType: Option[String],
                  val referenceKind: ReferenceKind = UnspecifiedReference) {

  private def notifyEvent(e: AMFEvent): Unit = compilerContext.parserContext.config.notifyEvent(e)

  def build()(implicit executionContext: ExecutionContext): Future[BaseUnit] = {
    compilerContext.logForFile(s"AMFCompiler#build: Building")
    if (compilerContext.hasCycles) failed(new CyclicReferenceException(compilerContext.fileContext.history))
    else
      compilerContext.runInCache(() => {
        compilerContext.logForFile(s"AMFCompiler#build: compiling")
        compile()
      })
  }

  private def compile()(implicit executionContext: ExecutionContext): Future[BaseUnit] = {
    for {
      content <- fetchContent()
      ast     <- Future.successful(parseSyntax(content))
      parsed  <- parseDomain(ast)
    } yield {
      parsed
    }
  }

  def autodetectSyntax(stream: CharSequence): Option[String] = {
    if (stream.length() > 2 && stream.charAt(0) == '#' && stream.charAt(1) == '%') {
      ExecutionLog.log(s"AMFCompiler#autodetectSyntax: auto detected application/yaml media type")
      Some("application/yaml")
    } else {
      compilerContext.platform.findCharInCharSequence(stream) { c =>
        c != '\n' && c != '\t' && c != '\r' && c != ' '
      } match {
        case Some(c) if c == '{' || c == '[' =>
          ExecutionLog.log(s"AMFCompiler#autodetectSyntax: auto detected application/json media type")
          Some("application/json")
        case _ => None
      }
    }
  }

  private[amf] def parseSyntax(input: Content): Either[Content, Root] = {
    compilerContext.logForFile("AMFCompiler#parseSyntax: parsing syntax")
    notifyEvent(StartingContentParsingEvent(compilerContext.path, input))
    val contentType: Option[String] = mediaType.flatMap(mt => new MediaTypeParser(mt).getSyntaxExp)
    val parsed: Option[(String, ParsedDocument)] = contentType
      .flatMap(mime => parseSyntaxForMediaType(input, mime))
      .orElse {
        contentType match {
          case None =>
            input.mime
              .flatMap(mime => parseSyntaxForMediaType(input, mime))
              .orElse {
                inferMediaTypeFromFileExtension(input).flatMap(inferred => parseSyntaxForMediaType(input, inferred))
              }
              .orElse {
                autodetectSyntax(input.stream).flatMap(inferred => parseSyntaxForMediaType(input, inferred))
              }
          case _ => None
        }
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
    // TODO ARM sort
    compilerContext.parserContext.config.sortedParseSyntax
      .find(_.applies(content.stream))
      .map(p => (mime, p.parse(content.stream, mime, compilerContext.parserContext)))
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
      case Left(content) =>
        mediaType match {
          // if is Left (empty or other error) and is root (context.history.length == 1), then return an error
          case Some(mime) if isRoot => throw new UnsupportedMediaTypeException(mime)
          case _                    => parseExternalFragment(content)
        }
      case Right(document) => parseDomain(document)
    }
  }

  private def isRoot = compilerContext.fileContext.history.length == 1

  private def parseDomain(document: Root)(implicit executionContext: ExecutionContext): Future[BaseUnit] = {
    compilerContext.logForFile("AMFCompiler#parseDomain: parsing domain")

    val domainPluginOption = getDomainPluginFor(document)

    val futureDocument: Future[BaseUnit] = domainPluginOption match {
      case Some(domainPlugin) =>
        compilerContext.logForFile(s"AMFCompiler#parseSyntax: parsing domain plugin ${domainPlugin.id}")
        parseReferences(document, domainPlugin) map { documentWithReferences =>
          val baseUnit =
            domainPlugin.parse(documentWithReferences, compilerContext.parserContext.copyWithSonsReferences())
          if (document.location == compilerContext.fileContext.root) baseUnit.withRoot(true)
          baseUnit.withRaw(document.raw).tagReferences(documentWithReferences)
        }
      case None =>
        Future.successful { compilerContext.parserContext.config.chooseFallback(document, mediaType) }
    }
    futureDocument map { unit =>
      // we setup the run for the parsed unit
      parsedModelEvent(unit)
      unit
    }
  }

  private def parsedModelEvent(baseUnit: BaseUnit): Unit = {
    compilerContext.logForFile("AMFCompiler#parseDomain: model ready")
    notifyEvent(ParsedModelEvent(compilerContext.path, baseUnit))
  }

  private[amf] def getDomainPluginFor(document: Root): Option[AMFParsePlugin] = {
    val allowed = filterByAllowed(compilerContext.parserContext.config.sortedParsePlugins,
                                  compilerContext.allowedMediaTypes.map(s => s ++ mediaType))
    allowed.find(_.applies(document))
  }

  /**
    * filters plugins that are allowed given the current compiler context.
    */
  private def filterByAllowed(plugins: Seq[AMFParsePlugin], allowed: Option[Seq[String]]): Seq[AMFParsePlugin] =
    allowed match {
      case Some(allowedList) => plugins.filter(_.mediaTypes.exists(allowedList.contains(_)))
      case None              => plugins
    }

  private[amf] def parseReferences(root: Root, domainPlugin: AMFParsePlugin)(
      implicit executionContext: ExecutionContext): Future[Root] = {
    val handler           = domainPlugin.referenceHandler(compilerContext.parserContext.eh)
    val allowedMediaTypes = domainPlugin.validMediaTypesToReference ++ domainPlugin.mediaTypes
    val refs              = handler.collect(root.parsed, compilerContext.parserContext)
    compilerContext.logForFile(s"AMFCompiler#parseReferences: ${refs.toReferences.size} references found")
    val parsed: Seq[Future[Option[ParsedReference]]] = refs.toReferences
      .filter(_.isRemote)
      .map { link =>
        val nodes = link.refs.map(_.node)
        link.resolve(compilerContext, allowedMediaTypes, domainPlugin.allowRecursiveReferences) flatMap {
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
      val parsePlugin = compilerContext.parserContext.config.sortedParsePlugins.find(_.applies(document))
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
  def init()(implicit executionContext: ExecutionContext) {
    // We register ourselves as the Runtime compiler
    RuntimeCompiler.register(new AMFCompilerAdapter())
  }
}

class AMFCompilerAdapter(implicit executionContext: ExecutionContext) extends RuntimeCompiler {
  override def build(compilerContext: CompilerContext,
                     mediaType: Option[String],
                     referenceKind: ReferenceKind): Future[BaseUnit] = {
    new AMFCompiler(compilerContext, mediaType, referenceKind).build()
  }
}

case class Root(parsed: ParsedDocument,
                location: String,
                mediatype: String,
                references: Seq[ParsedReference],
                referenceKind: ReferenceKind,
                raw: String) {}

object Root {
  def apply(parsed: ParsedDocument,
            location: String,
            mediatype: String,
            references: Seq[ParsedReference],
            referenceKind: ReferenceKind,
            raw: String): Root =
    new Root(parsed, location.normalizeUrl, mediatype, references, referenceKind, raw)
}
