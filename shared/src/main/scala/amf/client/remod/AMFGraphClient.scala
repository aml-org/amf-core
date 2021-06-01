package amf.client.remod

import amf.ProfileName
import amf.core.model.document.BaseUnit
import amf.core.validation.AMFValidationReport
import org.yaml.model.YDocument

import scala.concurrent.{ExecutionContext, Future}

/**
  * Contains common AMF graph operations.
  * Base client for <code>AMLClient</code> and <code>AMFClient</code>.
  */
class AMFGraphClient(protected val configuration: AMFGraphConfiguration) {

  implicit val exec: ExecutionContext = configuration.getExecutionContext

  def getConfiguration: AMFGraphConfiguration = configuration

  /**
    * Asynchronously generate a BaseUnit from the content located in the given url.
    * @param url Location of the file to parse
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parse(url: String): Future[AMFResult] = AMFParser.parse(url, configuration)

  /**
    * Asynchronously generate a BaseUnit from the content located in the given url.
    * @param url Location of the file to parse
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parse(url: String, mediaType: String): Future[AMFResult] = AMFParser.parse(url, mediaType, configuration)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The content as a string
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parseContent(content: String): Future[AMFResult] = AMFParser.parseContent(content, configuration)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The content as a string
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return A CompletableFuture of [[AMFResult]]
    */
  def parseContent(content: String, mediaType: String): Future[AMFResult] =
    AMFParser.parseContent(content, mediaType, configuration)

  /**
    * Transforms a [[BaseUnit]] with the default configuration
    * @param bu [[BaseUnit]] to transform
    * @return An [[AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(bu: BaseUnit): AMFResult = AMFTransformer.transform(bu, configuration) // clone? BaseUnit.resolved

  /**
    * Transforms a [[BaseUnit]] with a specific pipeline
    * @param bu [[BaseUnit]] to transform
    * @param pipelineName name of any custom or [[AMFGraphConfiguration.predefined predefined]] pipeline
    * @return An [[AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(bu: BaseUnit, pipelineName: String): AMFResult =
    AMFTransformer.transform(bu, pipelineName, configuration) // clone? BaseUnit.resolved

  /**
    * Render a [[BaseUnit]] to its default type
    * @param bu [[BaseUnit]] to be rendered
    * @return The content rendered
    */
  def render(bu: BaseUnit): String = AMFRenderer.render(bu, configuration)

  /**
    * Render a [[BaseUnit]] and return the AST
    * @param bu [[BaseUnit]] to be rendered
    * @return the AST as a [[YDocument]]
    */
  def renderAST(bu: BaseUnit): YDocument = AMFRenderer.renderAST(bu, configuration)

  /**
    * Render a [[BaseUnit]] to a certain mediaType
    * @param bu [[BaseUnit]] to be rendered
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return The content rendered
    */
  def render(bu: BaseUnit, mediaType: String): String = AMFRenderer.render(bu, mediaType, configuration)

  /**
    * Render a [[BaseUnit]] to a certain mediaType and return the AST
    * @param bu [[BaseUnit]] to be rendered
    * @param mediaType The nature and format of the given content. Must be <code>"application/spec"</code> or <code>"application/spec+syntax"</code>.
    *                  Examples: <code>"application/raml10"</code> or <code>"application/raml10+yaml"</code>
    * @return the AST as a [[YDocument]]
    */
  def renderAST(bu: BaseUnit, mediaType: String): YDocument = AMFRenderer.renderAST(bu, mediaType, configuration)

  /**
    * Validate a [[BaseUnit]] with its default validation profile name
    * @param bu [[BaseUnit]] to validate
    * @return an [[AMFValidationReport]]
    */
  def validate(bu: BaseUnit): Future[AMFValidationReport] = AMFValidator.validate(bu, configuration)

  /**
    * Validate a [[BaseUnit]] with a specific validation profile name
    * @param bu [[BaseUnit]] to validate
    * @param profileName the [[amf.ProfileName]] of the desired validation profile
    * @return an [[AMFValidationReport]]
    */
  def validate(bu: BaseUnit, profileName: ProfileName): Future[AMFValidationReport] =
    AMFValidator.validate(bu, profileName, configuration)
}
