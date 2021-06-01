package amf.client.remod

import amf.client.convert.CoreClientConverters.platform
import amf.internal.resource.{ResourceLoader, StringResourceLoader}

import scala.concurrent.Future

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
  def parseContent(content: String, configuration: AMFGraphConfiguration): Future[AMFResult] = ???
//    parseAsync(DEFAULT_DOCUMENT_URL, Some(fromStream(stream)), env)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The unit as a string
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @param configuration [[AMFGraphConfiguration]]
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parseContent(content: String, mediaType: String, configuration: AMFGraphConfiguration): Future[AMFResult] = ???
//    parseAsync(DEFAULT_DOCUMENT_URL, Some(fromStream(stream)))

  private[amf] def parseAsync(url: String,
                              mediaType: Option[String],
                              configuration: AMFGraphConfiguration): Future[AMFResult] = ???

  private def fromStream(url: String, stream: String): ResourceLoader =
    StringResourceLoader(platform.resolvePath(url), stream)

  private def fromStream(stream: String): ResourceLoader = fromStream(DEFAULT_DOCUMENT_URL, stream)

  private val DEFAULT_DOCUMENT_URL = "http://a.ml/amf/default_document"
}
