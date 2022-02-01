package amf.core.client.scala.validation.payload

import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.remote.InternalContent
import amf.core.internal.convert.CoreClientConverters._

import scala.concurrent.{ExecutionContext, Future}

object ShapeValidationConfiguration {
  def predefined(): ShapeValidationConfiguration = ShapeValidationConfiguration(AMFGraphConfiguration.predefined())
}

case class ShapeValidationConfiguration(private[amf] val config: AMFGraphConfiguration) {
  val eh: AMFErrorHandler                = config.errorHandlerProvider.errorHandler()
  val executionContext: ExecutionContext = config.getExecutionContext
  val maxYamlReferences: Option[Int]     = config.options.parsingOptions.maxYamlReferences

  // Necessary for Java XML Payload Validator
  def fetchContent(url: String): Future[Content] =
    config.resolvers.resolveContent(url).map(InternalContentMatcher.asClient(_))(config.getExecutionContext)
}
