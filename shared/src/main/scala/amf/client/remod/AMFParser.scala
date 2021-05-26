package amf.client.remod

import amf.{AmfProfile, ProfileName, UnknownProfile}
import amf.client.convert.CoreClientConverters.platform
import amf.core.remote.{Cache, Context}
import amf.core.services.RuntimeCompiler
import amf.core.validation.AMFValidationReport
import amf.internal.resource.{ResourceLoader, StringResourceLoader}

import scala.concurrent.{ExecutionContext, Future}

object AMFParser {

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url : Location of the api.
    * @param env: AnfEnvironment
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parse(url: String, env: AMFGraphConfiguration): Future[AMFResult] = parseAsync(url, None, env)

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url : Location of the api.
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  // check Vendor , only param? ParseParams?
  def parse(url: String, mediaType: String, env: AMFGraphConfiguration): Future[AMFResult] =
    parseAsync(url, Some(mediaType), env)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content: The unit as a string
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parseContent(content: String, env: AMFGraphConfiguration): Future[AMFResult] =
    parseContent(content, DEFAULT_DOCUMENT_URL, None, env)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param stream: The unit as a string
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parseContent(content: String, mediaType: String, env: AMFGraphConfiguration): Future[AMFResult] =
    parseContent(content, DEFAULT_DOCUMENT_URL, Some(mediaType), env)

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
    RuntimeCompiler(
        url,
        mediaType,
        Context(platform),
        cache = Cache(),
        parseConfig
    ) map { model =>
      val results = parseConfig.eh.getResults
      AMFResult(model, AMFValidationReport.forModel(model, results))
    }
  }

  private def fromStream(url: String, stream: String): ResourceLoader =
    StringResourceLoader(platform.resolvePath(url), stream)

  private val DEFAULT_DOCUMENT_URL = "http://a.ml/amf/default_document"
}
