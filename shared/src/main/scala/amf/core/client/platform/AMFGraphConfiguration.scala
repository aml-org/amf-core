package amf.core.client.platform

import amf.core.client.platform.config.{AMFEventListener, ParsingOptions, RenderOptions}
import amf.core.client.platform.errorhandling.ErrorHandlerProvider
import amf.core.client.platform.execution.BaseExecutionEnvironment
import amf.core.client.platform.reference.UnitCache
import amf.core.client.platform.resource.ResourceLoader
import amf.core.client.platform.transform.TransformationPipeline
import amf.core.client.platform.validation.payload.AMFShapePayloadValidationPlugin
import amf.core.client.platform.adoption.{IdAdopter, IdAdopterProvider}
import amf.core.client.scala.{AMFGraphConfiguration => InternalGraphConfiguration}
import amf.core.internal.convert.ClientErrorHandlerConverter._
import amf.core.internal.convert.CoreClientConverters._
import amf.core.internal.convert.TransformationPipelineConverter._
import amf.core.internal.convert.PayloadValidationPluginConverter._

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/** Base AMF configuration object */
@JSExportAll
class AMFGraphConfiguration private[amf] (private[amf] val _internal: InternalGraphConfiguration) {
  private implicit val ec: ExecutionContext = _internal.getExecutionContext

  /** Contains common AMF graph operations associated to documents */
  def baseUnitClient(): AMFGraphBaseUnitClient = new AMFGraphBaseUnitClient(this)

  /** Contains functionality associated with specific elements of the AMF model */
  def elementClient(): AMFGraphElementClient = new AMFGraphElementClient(this)

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
    _internal.withUnitCache(UnitCacheMatcher.asInternal(cache))

  def withTransformationPipeline(pipeline: TransformationPipeline): AMFGraphConfiguration =
    _internal.withTransformationPipeline(pipeline)

  def withEventListener(listener: AMFEventListener): AMFGraphConfiguration = _internal.withEventListener(listener)

  def withExecutionEnvironment(executionEnv: BaseExecutionEnvironment): AMFGraphConfiguration =
    _internal.withExecutionEnvironment(executionEnv._internal)

  def withShapePayloadPlugin(plugin: AMFShapePayloadValidationPlugin): AMFGraphConfiguration =
    _internal.withPlugin(PayloadValidationPluginMatcher.asInternal(plugin))

  def withIdAdopterProvider(idAdopterProvider: IdAdopterProvider): AMFGraphConfiguration =
    _internal.withIdAdopterProvider(idAdopterProvider)

  private[amf] def getExecutionContext: ExecutionContext = _internal.getExecutionContext

}

@JSExportAll
@JSExportTopLevel("AMFGraphConfiguration")
object AMFGraphConfiguration {
  def empty(): AMFGraphConfiguration = InternalGraphConfiguration.empty()

  /** Predefined AMF core environment */
  def predefined(): AMFGraphConfiguration = InternalGraphConfiguration.predefined()
}
