package amf.client.exported

import amf.client.convert.CoreClientConverters._
import amf.client.convert.TransformationPipelineConverter._
import amf.client.exported.config.{AMFEventListener, AMFLogger, ParsingOptions, RenderOptions}
import amf.client.exported.transform.TransformationPipeline
import amf.client.reference.UnitCache
import amf.client.remod.{AMFGraphConfiguration => InternalGraphConfiguration}
import amf.client.resolve.ClientErrorHandlerConverter._
import amf.client.resource.ResourceLoader

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/** Base AMF configuration object */
@JSExportAll
class AMFGraphConfiguration private[amf] (private[amf] val _internal: InternalGraphConfiguration) {
  private implicit val ec: ExecutionContext = _internal.getExecutionContext

  def createClient(): AMFGraphClient = new AMFGraphClient(this)

  def withParsingOptions(parsingOptions: ParsingOptions): AMFGraphConfiguration =
    _internal.withParsingOptions(parsingOptions)

  def withRenderOptions(renderOptions: RenderOptions): AMFGraphConfiguration =
    _internal.withRenderOptions(renderOptions)

  //TODO FIX EH
//  def withErrorHandlerProvider(provider: ErrorHandlerProvider): AMFGraphConfiguration =
//    _internal.withErrorHandlerProvider(() => provider.errorHandler())

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
    *   - AMF Resolvers [[amf.client.remod.amfcore.config.AMFResolvers.predefined predefined]]
    *   - Default error handler provider that will create a [[amf.client.parse.DefaultParserErrorHandler]]
    *   - Empty [[amf.client.remod.amfcore.registry.AMFRegistry]]
    *   - MutedLogger: [[amf.client.exported.config.MutedLogger]]
    *   - Without Any listener
    */
  def predefined(): AMFGraphConfiguration = InternalGraphConfiguration.predefined()
}
