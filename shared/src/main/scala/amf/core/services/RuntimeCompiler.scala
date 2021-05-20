package amf.core.services

import amf.client.remod.ParseConfiguration
import amf.client.remod.amfcore.config.{FinishedParsingEvent, StartingParsingEvent}
import amf.core.model.document.BaseUnit
import amf.core.parser.{ReferenceKind, UnspecifiedReference}
import amf.core.remote.{Cache, Context}
import amf.core.{CompilerContext, CompilerContextBuilder}

import scala.concurrent.{ExecutionContext, Future}

trait RuntimeCompiler {
  def build(compilerContext: CompilerContext,
            mediaType: Option[String],
            referenceKind: ReferenceKind): Future[BaseUnit]
}

object RuntimeCompiler {
  var compiler: Option[RuntimeCompiler] = None
  def register(runtimeCompiler: RuntimeCompiler): Unit = {
    compiler = Some(runtimeCompiler)
  }

  // interface used by amf-service
  def apply(url: String,
            mediaType: Option[String],
            base: Context,
            cache: Cache,
            parserConfig: ParseConfiguration,
            referenceKind: ReferenceKind = UnspecifiedReference): Future[BaseUnit] = {
    val context = new CompilerContextBuilder(url, base.platform, parserConfig)
      .withCache(cache)
      .withFileContext(base)
      .build()
    compiler match {
      case Some(runtimeCompiler) =>
        val startingParsingEvent = StartingParsingEvent(context.path, mediaType)
        parserConfig.notifyEvent(startingParsingEvent)
        implicit val executionContext: ExecutionContext = parserConfig.executionContext
        runtimeCompiler.build(context, mediaType, referenceKind) map { parsedUnit =>
          val finishedParsingEvent = FinishedParsingEvent(context.path, parsedUnit)
          parserConfig.notifyEvent(finishedParsingEvent)
          parsedUnit
        }
      case _ => throw new Exception("No registered runtime compiler")
    }
  }

  // could not add new environment in this method as it forces breaking changes in ReferenceHandler
  def forContext(compilerContext: CompilerContext,
                 mediaType: Option[String],
                 referenceKind: ReferenceKind = UnspecifiedReference)(
      implicit executionContext: ExecutionContext): Future[BaseUnit] = {
    compiler match {
      case Some(runtimeCompiler) => runtimeCompiler.build(compilerContext, mediaType, referenceKind)
      case _                     => throw new Exception("No registered runtime compiler")
    }
  }
}
