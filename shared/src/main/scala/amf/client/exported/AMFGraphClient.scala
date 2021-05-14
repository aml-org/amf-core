package amf.client.exported

import amf.ProfileName
import amf.client.convert.CoreClientConverters._
import amf.client.model.document.BaseUnit
import amf.client.validate.AMFValidationReport
import amf.client.remod.{AMFGraphClient => InternalAMFGraphClient}
import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class AMFGraphClient private[amf] (private val _internal: InternalAMFGraphClient) {

  private implicit val ec: ExecutionContext = _internal.getConfiguration.getExecutionContext

  @JSExportTopLevel("AMFGraphClient")
  def this(configuration: AMFGraphConfiguration) = {
    this(new InternalAMFGraphClient(configuration))
  }

  def getConfiguration: AMFGraphConfiguration = _internal.getConfiguration

  def parse(url: String): ClientFuture[AMFResult]                    = _internal.parse(url).asClient
  def parse(url: String, mediaType: String): ClientFuture[AMFResult] = _internal.parse(url, mediaType).asClient
  def parseContent(content: String): ClientFuture[AMFResult]         = _internal.parseContent(content).asClient
  def parseContent(content: String, mediaType: String): ClientFuture[AMFResult] =
    _internal.parseContent(content, mediaType).asClient

  def transform(bu: BaseUnit): AMFResult                       = _internal.transform(bu)
  def transform(bu: BaseUnit, pipelineName: String): AMFResult = _internal.transform(bu, pipelineName)

  def render(bu: BaseUnit): String                    = _internal.render(bu)
  def render(bu: BaseUnit, mediaType: String): String = _internal.render(bu, mediaType)

  def validate(bu: BaseUnit): AMFValidationReport                           = _internal.validate(bu)
  def validate(bu: BaseUnit, profileName: ProfileName): AMFValidationReport = _internal.validate(bu, profileName)
}
