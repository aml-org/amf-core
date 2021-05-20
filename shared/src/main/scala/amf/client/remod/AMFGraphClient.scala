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
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url Location of the file to parse
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parse(url: String): Future[AMFResult] = AMFParser.parse(url, configuration)

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url Location of the file to parse
    * @param mediaType The nature and format of the given content. must be <code>"application/spec"</code> and may also include format
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parse(url: String, mediaType: String): Future[AMFResult] = AMFParser.parse(url, mediaType, configuration)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The unit as a string
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parseContent(content: String): Future[AMFResult] = AMFParser.parseContent(content, configuration)

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The unit as a string
    * @param mediaType The nature and format of the given content. must be <code>"application/spec"</code> and may also include format
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    * @example <code>parseContent("example content", "application/oas20+yaml")</code>
    */
  def parseContent(content: String, mediaType: String): Future[AMFResult] =
    AMFParser.parseContent(content, mediaType, configuration)

  /**
    * Transforms a [[amf.core.model.document.BaseUnit]] with the default configuration
    * @param bu [[amf.core.model.document.BaseUnit]] to transform
    * @return An [[amf.client.remod.AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(bu: BaseUnit): AMFResult = AMFTransformer.transform(bu, configuration)

  /**
    * Transforms a [[amf.core.model.document.BaseUnit]] with a specific pipeline
    * @param bu [[amf.core.model.document.BaseUnit]] to transform
    * @param pipelineName name of any custom or [[AMFGraphConfiguration.predefined predefined]] pipeline
    * @return An [[amf.client.remod.AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(bu: BaseUnit, pipelineName: String): AMFResult =
    AMFTransformer.transform(bu, pipelineName, configuration)

  /**
    * Render a [[amf.core.model.document.BaseUnit]] to its default type
    * @param bu [[amf.core.model.document.BaseUnit]] to be rendered
    * @return the unit rendered
    */
  def render(bu: BaseUnit): String = AMFRenderer.render(bu, configuration)

  /**
    * Render a [[amf.core.model.document.BaseUnit]] and return the AST
    * @param bu [[amf.core.model.document.BaseUnit]] to be rendered
    * @return the AST as a [[org.yaml.model.YDocument]]
    */
  def renderAST(bu: BaseUnit): YDocument = AMFRenderer.renderAST(bu, configuration)

  /**
    * Render a [[amf.core.model.document.BaseUnit]] to a certain mediaType
    * @param bu [[amf.core.model.document.BaseUnit]] to be rendered
    * @param mediaType The nature and format of the given content. must be <code>"application/spec"</code> and may also include format
    * @return the unit rendered
    */
  def render(bu: BaseUnit, mediaType: String): String = AMFRenderer.render(bu, mediaType, configuration)

  /**
    * Render a [[amf.core.model.document.BaseUnit]] to a certain mediaType and return the AST
    * @param bu [[amf.core.model.document.BaseUnit]] to be rendered
    * @param mediaType The nature and format of the given content. must be <code>"application/spec"</code> and may also include format
    * @return the AST as a [[org.yaml.model.YDocument]]
    */
  def renderAST(bu: BaseUnit, mediaType: String): YDocument = AMFRenderer.renderAST(bu, mediaType, configuration)

  /**
    * Validate a [[amf.core.model.document.BaseUnit]] with its default validation profile
    * @param bu [[amf.core.model.document.BaseUnit]] to validate
    * @return an [[amf.core.validation.AMFValidationReport]]
    */
  def validate(bu: BaseUnit): AMFValidationReport = AMFValidator.validate(bu, configuration)

  /**
    * Validate a [[amf.core.model.document.BaseUnit]] with a specific validation profile
    * @param bu [[amf.core.model.document.BaseUnit]] to validate
    * @param profileName the [[amf.ProfileName]] of the desired validation profile
    * @return an [[amf.core.validation.AMFValidationReport]]
    */
  def validate(bu: BaseUnit, profileName: ProfileName): AMFValidationReport =
    AMFValidator.validate(bu, profileName, configuration)
}
