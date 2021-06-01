package amf.client.remod

import amf.client.convert.CoreClientConverters.platform
import amf.core.AMFCompiler
import amf.core.remote.{Cache, Context}
import amf.core.validation.AMFValidationReport
import amf.internal.resource.{ResourceLoader, StringResourceLoader}

import scala.concurrent.{ExecutionContext, Future}

object AMFParser {

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url Location of the api
    * @param configuration [[AMFGraphConfiguration]]
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parse(url: String, configuration: AMFGraphConfiguration): Future[AMFResult] =
    parseAsync(url, None, configuration)

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url Location of the api
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @param configuration [[AMFGraphConfiguration]]
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  // check Vendor , only param? ParseParams?
  def parse(url: String, mediaType: String, configuration: AMFGraphConfiguration): Future[AMFResult] =
    parseAsync(url, Some(mediaType), configuration)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The unit to parse as a string
    * @param configuration [[AMFGraphConfiguration]]
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parseContent(content: String, env: AMFGraphConfiguration): Future[AMFResult] =
    parseContent(content, DEFAULT_DOCUMENT_URL, None, env)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The unit as a string
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @param configuration [[AMFGraphConfiguration]]
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parseContent(content: String, mediaType: String, configuration: AMFGraphConfiguration): Future[AMFResult] =
    parseContent(content, DEFAULT_DOCUMENT_URL, Some(mediaType), configuration)

  private[amf] def parseContent(content: String,
                                url: String,
                                mediaType: Option[String],
                                env: AMFGraphConfiguration): Future[AMFResult] = {
    val loader     = fromStream(url, content)
    val withLoader = env.withResourceLoader(loader)
    parseAsync(url, mediaType, withLoader)
  }

  private[amf] def parseAsync(url: String,
                              mediaType: Option[String],
                              amfConfig: AMFGraphConfiguration): Future[AMFResult] = {
    val parseConfig                                 = amfConfig.parseConfiguration
    implicit val executionContext: ExecutionContext = parseConfig.executionContext
    AMFCompiler(url, mediaType, Context(platform), Cache(), parseConfig)
      .build()
      .map { model =>
        val results = parseConfig.eh.getResults
        AMFResult(model, results)
      }
  }

  private def fromStream(url: String, stream: String): ResourceLoader =
    StringResourceLoader(platform.resolvePath(url), stream)

  private val DEFAULT_DOCUMENT_URL = "http://a.ml/amf/default_document"
}
