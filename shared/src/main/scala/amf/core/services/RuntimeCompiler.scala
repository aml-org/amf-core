package amf.core.services

import amf.client.`new`.BaseEnvironment
import amf.client.parse.DefaultParserErrorHandler
import amf.core.client.ParsingOptions
import amf.core.model.document.BaseUnit
import amf.core.parser.errorhandler.AmfParserErrorHandler
import amf.core.parser.{ParserContext, ReferenceKind, UnspecifiedReference}
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote.{Cache, Context}
import amf.core.{CompilerContext, CompilerContextBuilder}
import amf.internal.environment.Environment

import scala.concurrent.{ExecutionContext, Future}

trait RuntimeCompiler {
  def build(compilerContext: CompilerContext,
           mediaType: Option[String],
            vendor: Option[String],
            referenceKind: ReferenceKind): Future[BaseUnit]
}

object RuntimeCompiler {
  var compiler: Option[RuntimeCompiler] = None
  def register(runtimeCompiler: RuntimeCompiler): Unit = {
    compiler = Some(runtimeCompiler)
  }

  def apply(url: String,
            mediaType: Option[String],
            vendor: Option[String],
            base: Context,
            cache: Cache,
            newEnv: BaseEnvironment,
            referenceKind: ReferenceKind = UnspecifiedReference,
            ctx: Option[ParserContext] = None,
            env: Environment = Environment(),
            errorHandler: AmfParserErrorHandler = DefaultParserErrorHandler.withRun())(implicit executionContext: ExecutionContext): Future[BaseUnit] = {

    val withValueOfLegacyEnv = BaseEnvironment.fromLegacy(newEnv, env)
    val context = new CompilerContextBuilder(url, base.platform,errorHandler).withCache(cache).withFileContext(base).build(withValueOfLegacyEnv)
    compiler match {
      case Some(runtimeCompiler) =>
        AMFPluginsRegistry.featurePlugins().foreach(_.onBeginParsingInvocation(url, mediaType))
        runtimeCompiler.build(context, mediaType, vendor,referenceKind) map {
          parsedUnit =>
            AMFPluginsRegistry.featurePlugins().foldLeft(parsedUnit) {
              case (parsed, plugin) =>
                plugin.onFinishedParsingInvocation(url, parsed)
            }
        }
      case _ => throw new Exception("No registered runtime compiler")
    }
  }

  // could not add new environment in this method as it forces breaking changes in ReferenceHandler
  def forContext(compilerContext: CompilerContext,
            mediaType: Option[String],
            vendor: Option[String],
            referenceKind: ReferenceKind = UnspecifiedReference)(implicit executionContext: ExecutionContext): Future[BaseUnit] = {
    compiler match {
      case Some(runtimeCompiler) =>
        AMFPluginsRegistry.featurePlugins().foreach(_.onBeginParsingInvocation(compilerContext.path, mediaType))
        runtimeCompiler.build(compilerContext,mediaType, vendor, referenceKind) map {
          parsedUnit =>
            AMFPluginsRegistry.featurePlugins().foldLeft(parsedUnit) {
              case (parsed, plugin) =>
                plugin.onFinishedParsingInvocation(compilerContext.path, parsed)
            }
        }
      case _ => throw new Exception("No registered runtime compiler")
    }
  }
}
