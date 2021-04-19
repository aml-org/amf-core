package amf.client.remod

import amf.ProfileName
import amf.client.convert.CoreClientConverters.platform
import amf.client.parse.DefaultParserErrorHandler
import amf.core.client.ParsingOptions
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.remote.{Cache, Context, Vendor}
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.services.RuntimeCompiler
import amf.core.validation.AMFValidationReport

import scala.concurrent.{ExecutionContext, Future}

// client
private[amf] class AMFGraphClient(protected val configuration: AMFGraphConfiguration) {

  implicit val exec: ExecutionContext = configuration.getExecutionContext

  def getConfiguration: AMFGraphConfiguration = configuration
  // how do we return the ErrorHandler that is created by the provider?
  // relation between EH vs BU? should the BU have an EH inside?
  // should parse always return error handler + base unit? (Amf result)

  // sync or async?

  // content type format, pendiente
  def parse(url: String): Future[AMFResult] = AMFParser.parse(url, configuration)
  // build parsing context?

  def transform(bu: BaseUnit): AMFResult = AMFTransformer.transform(bu, configuration) // clone? BaseUnit.resolved

  def render(bu: BaseUnit): String = AMFRenderer.render(bu, configuration)

  def render(bu: BaseUnit, mediaType: String): String = AMFRenderer.render(bu, mediaType, configuration)
  // bu.clone?
  def validate(bu: BaseUnit, profileName: ProfileName): AMFValidationReport = ???

  // render

}
