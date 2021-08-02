package amf.core.client.scala

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.AMFParser
import amf.core.client.scala.parse.document.ParsedDocument
import amf.core.client.scala.render.AMFRenderer
import amf.core.client.scala.transform.AMFTransformer
import amf.core.client.scala.validation.{AMFValidationReport, AMFValidator}
import org.yaml.builder.DocBuilder

import scala.concurrent.{ExecutionContext, Future}

/**
  * Contains common AMF graph operations associated to documents.
  * Base client for <code>AMLBaseUnitClient</code> and <code>AMFBaseUnitClient</code>.
  */
class AMFGraphBaseUnitClient private[amf] (protected val configuration: AMFGraphConfiguration) {

  implicit val exec: ExecutionContext = configuration.getExecutionContext

  def getConfiguration: AMFGraphConfiguration = configuration

  /**
    * Asynchronously generate a BaseUnit from the content located in the given url.
    * @param url Location of the file to parse
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parse(url: String): Future[AMFParseResult] = AMFParser.parse(url, configuration)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The content as a string
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parseContent(content: String): Future[AMFParseResult] = AMFParser.parseContent(content, configuration)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The content as a string
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parseContent(content: String, mediaType: String): Future[AMFParseResult] =
    AMFParser.parseContent(content, mediaType, configuration)

  /**
    * Transforms a [[BaseUnit]] with the default configuration
    * @param baseUnit [[BaseUnit]] to transform
    * @return An [[AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(baseUnit: BaseUnit): AMFResult =
    AMFTransformer.transform(baseUnit, configuration) // clone? BaseUnit.resolved

  /**
    * Transforms a [[BaseUnit]] with a specific pipeline
    * @param baseUnit [[BaseUnit]] to transform
    * @param pipeline name of any custom or [[AMFGraphConfiguration.predefined predefined]] pipeline
    * @return An [[AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(baseUnit: BaseUnit, pipeline: String): AMFResult =
    AMFTransformer.transform(baseUnit, pipeline, configuration) // clone? BaseUnit.resolved

  /**
    * Render a [[BaseUnit]] to its default type
    * @param baseUnit [[BaseUnit]] to be rendered
    * @return The content rendered
    */
  def render(baseUnit: BaseUnit): String = AMFRenderer.render(baseUnit, configuration)

  /**
    * Render a [[BaseUnit]] and return the AST
    * @param baseUnit [[BaseUnit]] to be rendered
    * @return the AST as a [[ParsedDocument]]
    */
  def renderAST(baseUnit: BaseUnit): ParsedDocument = AMFRenderer.renderAST(baseUnit, configuration)

  /**
    * Render a [[BaseUnit]] to a certain mediaType
    * @param baseUnit [[BaseUnit]] to be rendered
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return The content rendered
    */
  def render(baseUnit: BaseUnit, mediaType: String): String = AMFRenderer.render(baseUnit, mediaType, configuration)

  /**
    * Render a [[BaseUnit]] to a certain mediaType and return the AST
    * @param baseUnit [[BaseUnit]] to be rendered
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return the AST as a [[ParsedDocument]]
    */
  def renderAST(baseUnit: BaseUnit, mediaType: String): ParsedDocument =
    AMFRenderer.renderAST(baseUnit, mediaType, configuration)

  /**
    * Render a [[BaseUnit]] to a [[DocBuilder]] in the form of a graph (jsonld)
    * @param baseUnit [[BaseUnit]] to be rendered
    * @param builder [[DocBuilder]] which is used for rendering
    * @return The result produced by the DocBuilder after rendering
    */
  def renderGraphToBuilder[T](baseUnit: BaseUnit, builder: DocBuilder[T]): T =
    AMFRenderer.renderGraphToBuilder(baseUnit, builder, configuration)

  /**
    * Validate a [[BaseUnit]] with its default validation profile name
    * @param baseUnit [[BaseUnit]] to validate
    * @return an [[AMFValidationReport]]
    */
  def validate(bu: BaseUnit): Future[AMFValidationReport] = AMFValidator.validate(bu, configuration)
}
