package amf.core.client.platform

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.platform.model.document.BaseUnit
import amf.core.client.scala.{AMFGraphBaseUnitClient => InternalAMFGraphBaseUnitClient}
import amf.core.client.platform.validation.AMFValidationReport
import amf.core.client.common.validation.ProfileName
import org.yaml.builder.DocBuilder
import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Contains common AMF graph operations.
  * Base client for <code>AMLClient</code> and <code>AMFClient</code>.
  */
@JSExportAll
class AMFGraphBaseUnitClient private[amf] (private val _internal: InternalAMFGraphBaseUnitClient) {

  private[amf] def this(configuration: AMFGraphConfiguration) = {
    this(new InternalAMFGraphBaseUnitClient(configuration))
  }

  private implicit val ec: ExecutionContext = _internal.getConfiguration.getExecutionContext

  def getConfiguration(): AMFGraphConfiguration = _internal.getConfiguration

  /**
    * Asynchronously generate a BaseUnit from the content located in the given url.
    * @param url Location of the file to parse
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parse(url: String): ClientFuture[AMFResult] = _internal.parse(url).asClient

  /**
    * Asynchronously generate a BaseUnit from the content located in the given url.
    * @param url Location of the file to parse
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parse(url: String, mediaType: String): ClientFuture[AMFResult] = _internal.parse(url, mediaType).asClient

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The content as a string
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parseContent(content: String): ClientFuture[AMFResult] = _internal.parseContent(content).asClient

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The content as a string
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parseContent(content: String, mediaType: String): ClientFuture[AMFResult] =
    _internal.parseContent(content, mediaType).asClient

  /**
    * Transforms a [[BaseUnit]] with the default configuration
    * @param bu [[BaseUnit]] to transform
    * @return An [[AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(bu: BaseUnit): AMFResult = _internal.transform(bu)

  /**
    * Transforms a [[BaseUnit]] with a specific pipeline
    * @param bu [[BaseUnit]] to transform
    * @param pipelineName name of any custom or [[AMFGraphConfiguration.predefined predefined]] pipeline
    * @return An [[AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(bu: BaseUnit, pipelineName: String): AMFResult = _internal.transform(bu, pipelineName)

  /**
    * Render a [[BaseUnit]] to its default type
    * @param bu [[BaseUnit]] to be rendered
    * @return The content rendered
    */
  def render(bu: BaseUnit): String = _internal.render(bu)

  /**
    * Render a [[BaseUnit]] to a certain mediaType
    * @param bu [[BaseUnit]] to be rendered
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return The content rendered
    */
  def render(bu: BaseUnit, mediaType: String): String = _internal.render(bu, mediaType)

  /**
    * Render a [[BaseUnit]] to a [[DocBuilder]] in the form of a graph (jsonld)
    * @param bu [[BaseUnit]] to be rendered
    * @param builder [[DocBuilder]] which is used for rendering
    * @return The result produced by the DocBuilder after rendering
    */
  def renderGraphToBuilder[T](bu: BaseUnit, builder: DocBuilder[T]): T = _internal.renderGraphToBuilder(bu, builder)

  /**
    * Validate a [[BaseUnit]] with its default validation profile name
    * @param bu [[BaseUnit]] to validate
    * @return an [[AMFValidationReport]]
    */
  def validate(bu: BaseUnit): ClientFuture[AMFValidationReport] = _internal.validate(bu).asClient

  /**
    * Validate a [[BaseUnit]] with a specific validation profile name
    * @param bu [[BaseUnit]] to validate
    * @param profileName the [[ProfileName]] of the desired validation profile
    * @return an [[AMFValidationReport]]
    */
  def validate(bu: BaseUnit, profileName: ProfileName): ClientFuture[AMFValidationReport] =
    _internal.validate(bu, profileName).asClient
}
