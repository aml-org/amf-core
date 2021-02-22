package amf.client.`new`

import amf.{MessageStyle, ProfileName, RAMLStyle}
import amf.client.convert.CoreClientConverters.{ClientFuture, platform}
import amf.client.environment.DefaultEnvironment
import amf.client.model.document.BaseUnit
import amf.client.parse.DefaultParserErrorHandler
import amf.client.validate.ValidationReport
import amf.core.client.ParsingOptions
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.remote.{Aml, Cache, Context, Vendor}
import amf.core.services.{RuntimeCompiler, RuntimeValidator}
import amf.internal.environment
import amf.internal.resource.{ResourceLoader, StringResourceLoader}

import scala.concurrent.Future
import scala.scalajs.js.annotation.JSExport

object AmfParser {


  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url : Location of the api.
    * @param env: AnfEnvironment
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parse(url: String, env:BaseEnvironment): Future[AmfResult] = parseAsync(url, None, env)

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url : Location of the api.
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */

  // check Vendor , only param? ParseParams?
  def parse(url: String, vendor:Vendor, env:BaseEnvironment): Future[AmfResult] = parseAsync(url, Some(vendor), env)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param stream: The unit as a string
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  @JSExport
  def parseStream(stream: String, env:AmfEnvironment): Future[AmfResult] = ???
//    parseAsync(DEFAULT_DOCUMENT_URL, Some(fromStream(stream)), env)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param stream: The unit as a string
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  @JSExport
  def parseStream(stream: String,vendor:Vendor, env:AmfEnvironment): Future[AmfResult] = {
    ???
//    parseAsync(DEFAULT_DOCUMENT_URL, Some(fromStream(stream)))
  }

  // should be in object specific to AML
//  def parseInstance(url: String, definedBy: String, env:AmfEnvironment): Future[AmfResult] = {
//    parse(definedBy, env).map { d =>
//      d.bu  match {
//        case d:Dialect => parseInstance(url, d, env)
//        case _ => throw Exception //?
//      }
//    }
//  }
//
//  def parseInstance(url:String, definedBy:Dialect, env:AmfEnvironment)= {
//    env.registry.withDialect(d).getInstance().parse(url, Some(Aml))
//
//  }

  private[amf] def parseAsync(url: String,
                              vendor:Option[Vendor],
                              amfEnvironment: BaseEnvironment): Future[AmfResult] = {
//    amfEnvironment.beforeParse().flatMap { _ =>
//      val environment = {
//        val e = internalEnv()
//        loader.map(e.add).getOrElse(e)
//      }
//
//      RuntimeCompiler(
//        url,
//        Option(mediaType),
//        Some(vendor),
//        Context(platform),
//        env = environment,
//        cache = Cache(),
//        parsingOptions = parsingOptions,
//        errorHandler = DefaultParserErrorHandler.withRun()
//      ) map { model =>
//        parsedModel = Some(model)
//        model
//      }
//    }
    ???
  }

  private def fromStream(url: String, stream: String): ResourceLoader =
    StringResourceLoader(platform.resolvePath(url), stream)

  private def fromStream(stream: String): ResourceLoader = fromStream(DEFAULT_DOCUMENT_URL, stream)

  private val DEFAULT_DOCUMENT_URL = "http://a.ml/amf/default_document"
}
