package amf.core.client.platform

import amf.core.client.common.validation.ProfileName
import amf.core.client.platform.model.document.BaseUnit
import amf.core.client.platform.validation.AMFValidationReport
import amf.core.client.scala.{AMFGraphBaseUnitClient => InternalAMFGraphBaseUnitClient}
import amf.core.internal.convert.CoreClientConverters._
import org.yaml.builder.DocBuilder

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.JSExportAll

/**
  * Contains common AMF graph operations associated to documents.
  * Base client for <code>AMLBaseUnitClient</code> and <code>AMFBaseUnitClient</code>.
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
  def parse(url: String): ClientFuture[AMFParseResult] = _internal.parse(url).asClient

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The content as a string
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parseContent(content: String): ClientFuture[AMFParseResult] = _internal.parseContent(content).asClient

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The content as a string
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parseContent(content: String, mediaType: String): ClientFuture[AMFParseResult] =
    _internal.parseContent(content, mediaType).asClient

  /**
    * Transforms a [[BaseUnit]] with the default configuration
    * @param baseUnit [[BaseUnit]] to transform
    * @return An [[AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(baseUnit: BaseUnit): AMFResult = _internal.transform(baseUnit)

  /**
    * Transforms a [[BaseUnit]] with a specific pipeline
    * @param baseUnit [[BaseUnit]] to transform
    * @param pipeline name of any custom or [[AMFGraphConfiguration.predefined predefined]] pipeline
    * @return An [[AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(baseUnit: BaseUnit, pipeline: String): AMFResult = _internal.transform(baseUnit, pipeline)

  /**
    * Render a [[BaseUnit]] to its default type
    * @param baseUnit [[BaseUnit]] to be rendered
    * @return The content rendered
    */
  def render(baseUnit: BaseUnit): String = _internal.render(baseUnit)

  /**
    * Render a [[BaseUnit]] to a certain mediaType
    * @param baseUnit [[BaseUnit]] to be rendered
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return The content rendered
    */
  def render(baseUnit: BaseUnit, mediaType: String): String = _internal.render(baseUnit, mediaType)

  /**
    * Render a [[BaseUnit]] to a [[DocBuilder]] in the form of a graph (jsonld)
    * @param baseUnit [[BaseUnit]] to be rendered
    * @param builder [[DocBuilder]] which is used for rendering
    * @return The result produced by the DocBuilder after rendering
    */
  def renderGraphToBuilder[T](baseUnit: BaseUnit, builder: DocBuilder[T]): T =
    _internal.renderGraphToBuilder(baseUnit, builder)

  /**
    * Validate a [[BaseUnit]] with its default validation profile name
    * @param bu [[BaseUnit]] to validate
    * @return an [[AMFValidationReport]]
    */
  def validate(bu: BaseUnit): ClientFuture[AMFValidationReport] = _internal.validate(bu).asClient
}
