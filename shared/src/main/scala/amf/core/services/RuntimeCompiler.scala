package amf.core.services

import amf.client.remod.ParseConfiguration
import amf.client.remod.amfcore.config.{FinishedParsingEvent, StartingParsingEvent}
import amf.core.model.document.BaseUnit
import amf.core.parser.{ReferenceKind, UnspecifiedReference}
import amf.core.remote.{Cache, Context}
import amf.core.{AMFCompilerAdapter, CompilerContext, CompilerContextBuilder}

import scala.concurrent.{ExecutionContext, Future}

trait RuntimeCompiler {
  def build(compilerContext: CompilerContext,
            mediaType: Option[String],
            referenceKind: ReferenceKind): Future[BaseUnit]
}

object RuntimeCompiler {

  // interface used by amf-service
  def apply(url: String,
            mediaType: Option[String],
            base: Context,
            cache: Cache,
            parserConfig: ParseConfiguration,
            referenceKind: ReferenceKind = UnspecifiedReference): Future[BaseUnit] = {
    implicit val executionContext: ExecutionContext = parserConfig.executionContext
    val context = new CompilerContextBuilder(url, base.platform, parserConfig)
      .withCache(cache)
      .withFileContext(base)
      .build()
    val runtimeCompiler      = new AMFCompilerAdapter()
    val startingParsingEvent = StartingParsingEvent(context.path, mediaType)
    parserConfig.notifyEvent(startingParsingEvent)
    runtimeCompiler.build(context, mediaType, referenceKind) map { parsedUnit =>
      val finishedParsingEvent = FinishedParsingEvent(context.path, parsedUnit)
      parserConfig.notifyEvent(finishedParsingEvent)
      parsedUnit
    }
  }

  // could not add new environment in this method as it forces breaking changes in ReferenceHandler
  def forContext(compilerContext: CompilerContext,
                 mediaType: Option[String],
                 referenceKind: ReferenceKind = UnspecifiedReference)(
      implicit executionContext: ExecutionContext): Future[BaseUnit] = {
    val runtimeCompiler = new AMFCompilerAdapter()
    runtimeCompiler.build(compilerContext, mediaType, referenceKind)
  }
}
