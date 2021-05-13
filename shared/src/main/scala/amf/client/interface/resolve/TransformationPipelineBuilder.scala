package amf.client.interface.resolve
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.remod.amfcore.resolution.{TransformationPipelineBuilder => InternalTransformationPipelineBuilder}
import amf.client.convert.CoreClientConverters._
import amf.client.convert.TransformationPipelineConverter._
import amf.client.remod.AMFGraphConfiguration

@JSExportAll
case class TransformationPipelineBuilder(private[amf] val _internal: InternalTransformationPipelineBuilder) {

  def build(): TransformationPipeline = _internal.build()

  def withName(newName: String): TransformationPipelineBuilder = _internal.withName(newName)

  /**
    * inserts stage at the end of the pipeline
    */
  def append(newStage: TransformationStep): TransformationPipelineBuilder = _internal.append(newStage)

  /**
    * inserts stage at the beginning of pipeline
    */
  def prepend(newStage: TransformationStep): TransformationPipelineBuilder = _internal.prepend(newStage)
}

@JSExportTopLevel("TransformationPipelineBuilder")
@JSExportAll
object TransformationPipelineBuilder {

  def empty(pipelineName: String): TransformationPipelineBuilder =
    InternalTransformationPipelineBuilder.empty(pipelineName)

  def fromPipeline(pipeline: TransformationPipeline): TransformationPipelineBuilder =
    InternalTransformationPipelineBuilder.fromPipeline(pipeline)

  def fromPipeline(pipelineName: String, conf: AMFGraphConfiguration): ClientOption[TransformationPipelineBuilder] =
    InternalTransformationPipelineBuilder.fromPipeline(pipelineName, conf).asClient
}
