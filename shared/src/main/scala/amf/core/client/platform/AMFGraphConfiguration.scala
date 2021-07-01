package amf.core.client.platform

import amf.core.client.platform.config.{AMFEventListener, AMFLogger, ParsingOptions, RenderOptions}
import amf.core.client.platform.errorhandling.ErrorHandlerProvider
import amf.core.client.platform.execution.BaseExecutionEnvironment
import amf.core.client.platform.reference.UnitCache
import amf.core.client.platform.resource.ResourceLoader
import amf.core.client.platform.transform.TransformationPipeline
import amf.core.client.platform.validation.payload.ShapePayloadValidatorFactory
import amf.core.internal.convert.ClientErrorHandlerConverter._
import amf.core.internal.convert.CoreClientConverters._
import amf.core.internal.convert.TransformationPipelineConverter._

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.core.client.scala.{AMFGraphConfiguration => InternalGraphConfiguration}
import amf.core.internal.convert.CoreClientConverters

/** Base AMF configuration object */
@JSExportAll
class AMFGraphConfiguration private[amf] (private[amf] val _internal: InternalGraphConfiguration) {
  private implicit val ec: ExecutionContext = _internal.getExecutionContext

  def baseUnitClient(): AMFGraphBaseUnitClient = new AMFGraphBaseUnitClient(this)

  def payloadValidatorFactory(): ShapePayloadValidatorFactory =
    ShapePayloadValidatorFactoryMatcher.asClient(_internal.payloadValidatorFactory())

  def withParsingOptions(parsingOptions: ParsingOptions): AMFGraphConfiguration =
    _internal.withParsingOptions(parsingOptions)

  def withRenderOptions(renderOptions: RenderOptions): AMFGraphConfiguration =
    _internal.withRenderOptions(renderOptions)

  def withErrorHandlerProvider(provider: ErrorHandlerProvider): AMFGraphConfiguration =
    _internal.withErrorHandlerProvider(() => provider.errorHandler())

  def withResourceLoader(rl: ResourceLoader): AMFGraphConfiguration =
    _internal.withResourceLoader(ResourceLoaderMatcher.asInternal(rl))

  def withResourceLoaders(rl: ClientList[ResourceLoader]): AMFGraphConfiguration =
    _internal.withResourceLoaders(rl.asInternal.toList)

  def withUnitCache(cache: UnitCache): AMFGraphConfiguration =
    _internal.withUnitCache(ReferenceResolverMatcher.asInternal(cache))

  def withTransformationPipeline(pipeline: TransformationPipeline): AMFGraphConfiguration =
    _internal.withTransformationPipeline(pipeline)

  def withEventListener(listener: AMFEventListener): AMFGraphConfiguration = _internal.withEventListener(listener)

  def withLogger(logger: AMFLogger): AMFGraphConfiguration = _internal.withLogger(logger)

  def withExecutionEnvironment(executionEnv: BaseExecutionEnvironment): AMFGraphConfiguration =
    _internal.withExecutionEnvironment(executionEnv._internal)

  /**
    * Merges two environments taking into account specific attributes that can be merged.
    * This is currently limited to: registry plugins, registry transformation pipelines.
    */
  def merge(other: AMFGraphConfiguration): AMFGraphConfiguration = _internal.merge(other)

  private[amf] def getExecutionContext: ExecutionContext = _internal.getExecutionContext

}

@JSExportAll
@JSExportTopLevel("AMFGraphConfiguration")
object AMFGraphConfiguration {
  def empty(): AMFGraphConfiguration = InternalGraphConfiguration.empty()

  /**
    * Predefined AMF core environment with:
    *   - AMF Resolvers [[AMFResolvers.predefined predefined]]
    *   - Default error handler provider that will create a [[amf.core.client.scala.errorhandling.DefaultErrorHandler]]
    *   - Empty [[AMFRegistry]]
    *   - MutedLogger: [[amf.core.client.platform.config.MutedLogger]]
    *   - Without Any listener
    */
  def predefined(): AMFGraphConfiguration = InternalGraphConfiguration.predefined()
}
