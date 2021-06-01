package amf.client.remod

import amf.client.convert.CoreClientConverters.platform
import amf.internal.resource.{ResourceLoader, StringResourceLoader}

import scala.concurrent.Future

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
  def parseContent(content: String, env: AMFGraphConfiguration): Future[AMFResult] = ???
//    parseAsync(DEFAULT_DOCUMENT_URL, Some(fromStream(stream)), env)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param stream: The unit as a string
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parseContent(content: String, mediaType: String, env: AMFGraphConfiguration): Future[AMFResult] = ???
//    parseAsync(DEFAULT_DOCUMENT_URL, Some(fromStream(stream)))

  private[amf] def parseAsync(url: String,
                              mediaType: Option[String],
                              amfEnvironment: AMFGraphConfiguration): Future[AMFResult] = ???

  private def fromStream(url: String, stream: String): ResourceLoader =
    StringResourceLoader(platform.resolvePath(url), stream)

  private def fromStream(stream: String): ResourceLoader = fromStream(DEFAULT_DOCUMENT_URL, stream)

  private val DEFAULT_DOCUMENT_URL = "http://a.ml/amf/default_document"
}
