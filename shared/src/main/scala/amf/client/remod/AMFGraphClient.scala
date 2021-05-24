package amf.client.remod

import amf.ProfileName
import amf.core.model.document.BaseUnit
import amf.core.validation.AMFValidationReport
import org.yaml.model.YDocument

import scala.concurrent.{ExecutionContext, Future}

class AMFGraphClient(protected val configuration: AMFGraphConfiguration) {

  implicit val exec: ExecutionContext = configuration.getExecutionContext

  def getConfiguration: AMFGraphConfiguration = configuration

  def parse(url: String): Future[AMFResult]                    = AMFParser.parse(url, configuration)
  def parse(url: String, mediaType: String): Future[AMFResult] = AMFParser.parse(url, mediaType, configuration)
  def parseContent(content: String): Future[AMFResult]         = AMFParser.parseContent(content, configuration)
  def parseContent(content: String, mediaType: String): Future[AMFResult] =
    AMFParser.parseContent(content, mediaType, configuration)

  def transform(bu: BaseUnit): AMFResult = AMFTransformer.transform(bu, configuration) // clone? BaseUnit.resolved
  def transform(bu: BaseUnit, pipelineName: String): AMFResult =
    AMFTransformer.transform(bu, pipelineName, configuration) // clone? BaseUnit.resolved

  def render(bu: BaseUnit): Future[String]                    = AMFRenderer.render(bu, configuration)
  def renderAST(bu: BaseUnit): YDocument                      = AMFRenderer.renderAST(bu, configuration)
  def render(bu: BaseUnit, mediaType: String): Future[String] = AMFRenderer.render(bu, mediaType, configuration)
  def renderAST(bu: BaseUnit, mediaType: String): YDocument   = AMFRenderer.renderAST(bu, mediaType, configuration)

  def validate(bu: BaseUnit): AMFValidationReport = AMFValidator.validate(bu, configuration)
  def validate(bu: BaseUnit, profileName: ProfileName): AMFValidationReport =
    AMFValidator.validate(bu, profileName, configuration)
}
