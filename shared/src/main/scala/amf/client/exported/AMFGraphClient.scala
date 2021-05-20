package amf.client.exported

import amf.ProfileName
import amf.client.convert.CoreClientConverters._
import amf.client.model.document.BaseUnit
import amf.client.remod.{AMFGraphClient => InternalAMFGraphClient}
import amf.client.validate.AMFValidationReport

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Contains common AMF graph operations.
  * Base client for <code>AMLClient</code> and <code>AMFClient</code>.
  */
@JSExportAll
class AMFGraphClient private[amf] (private val _internal: InternalAMFGraphClient) {

  private implicit val ec: ExecutionContext = _internal.getConfiguration.getExecutionContext

  @JSExportTopLevel("AMFGraphClient")
  def this(configuration: AMFGraphConfiguration) = {
    this(new InternalAMFGraphClient(configuration))
  }

  def getConfiguration: AMFGraphConfiguration = _internal.getConfiguration

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url Location of the file to parse
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parse(url: String): ClientFuture[AMFResult] = _internal.parse(url).asClient

  /**
    * Asynchronously generate a BaseUnit from the unit located in the given url.
    * @param url Location of the file to parse
    * @param mediaType The nature and format of the given content. must be <code>"application/spec"</code> and may also include format
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parse(url: String, mediaType: String): ClientFuture[AMFResult] = _internal.parse(url, mediaType).asClient

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The unit as a string
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    */
  def parseContent(content: String): ClientFuture[AMFResult] = _internal.parseContent(content).asClient

  /**
    * Asynchronously generate a BaseUnit from a given string.
    * @param content The unit as a string
    * @param mediaType The nature and format of the given content. must be <code>"application/spec"</code> and may also include format
    * @return A future that will have a BaseUnit or an error to handle the result of such invocation.
    * @example <code>parseContent("example content", "application/oas20+yaml")</code>
    */
  def parseContent(content: String, mediaType: String): ClientFuture[AMFResult] =
    _internal.parseContent(content, mediaType).asClient

  /**
    * Transforms a [[amf.client.model.document.BaseUnit]] with the default configuration
    * @param bu [[amf.client.model.document.BaseUnit]] to transform
    * @return An [[amf.client.exported.AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(bu: BaseUnit): AMFResult = _internal.transform(bu)

  /**
    * Transforms a [[amf.client.model.document.BaseUnit]] with a specific pipeline
    * @param bu [[amf.client.model.document.BaseUnit]] to transform
    * @param pipelineName name of any custom or [[AMFGraphConfiguration.predefined predefined]] pipeline
    * @return An [[amf.client.exported.AMFResult]] with the transformed BaseUnit and it's report
    */
  def transform(bu: BaseUnit, pipelineName: String): AMFResult = _internal.transform(bu, pipelineName)

  /**
    * Render a [[amf.client.model.document.BaseUnit]] to its default type
    * @param bu [[amf.client.model.document.BaseUnit]] to be rendered
    * @return the unit rendered
    */
  def render(bu: BaseUnit): String = _internal.render(bu)

  /**
    * Render a [[amf.client.model.document.BaseUnit]] to a certain mediaType
    * @param bu [[amf.client.model.document.BaseUnit]] to be rendered
    * @param mediaType The nature and format of the given content. must be <code>"application/spec"</code> and may also include format
    * @return the unit rendered
    */
  def render(bu: BaseUnit, mediaType: String): String = _internal.render(bu, mediaType)

  /**
    * Validate a [[amf.client.model.document.BaseUnit]] with its default validation profile
    * @param bu [[amf.client.model.document.BaseUnit]] to validate
    * @return an [[amf.client.validate.AMFValidationReport]]
    */
  def validate(bu: BaseUnit): AMFValidationReport = _internal.validate(bu)

  /**
    * Validate a [[amf.client.model.document.BaseUnit]] with a specific validation profile
    * @param bu [[amf.client.model.document.BaseUnit]] to validate
    * @param profileName the [[amf.ProfileName]] of the desired validation profile
    * @return an [[amf.client.validate.AMFValidationReport]]
    */
  def validate(bu: BaseUnit, profileName: ProfileName): AMFValidationReport = _internal.validate(bu, profileName)
}
