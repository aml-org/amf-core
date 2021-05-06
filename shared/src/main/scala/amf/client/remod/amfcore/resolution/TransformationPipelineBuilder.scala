package amf.client.remod.amfcore.resolution

import amf.client.remod.AMFGraphConfiguration
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.TransformationStep

case class TransformationPipelineBuilder private (builderName: String, builderSteps: Seq[TransformationStep] = Nil) {

  def build(): TransformationPipeline = new TransformationPipeline {
    override val name: String                   = builderName
    override def steps: Seq[TransformationStep] = builderSteps
  }

  def withName(newName: String): TransformationPipelineBuilder = {
    this.copy(builderName = newName)
  }

  /**
    * inserts stage at the end of the pipeline
    */
  def append(newStage: TransformationStep): TransformationPipelineBuilder = {
    this.copy(builderSteps = builderSteps :+ newStage)

  }

  /**
    * inserts stage at the beginning of pipeline
    */
  def prepend(newStage: TransformationStep): TransformationPipelineBuilder = {
    this.copy(builderSteps = newStage +: builderSteps)
  }

}

object TransformationPipelineBuilder {

  def empty(pipelineName: String): TransformationPipelineBuilder = new TransformationPipelineBuilder(pipelineName)

  def fromPipeline(pipeline: TransformationPipeline): TransformationPipelineBuilder = {
    new TransformationPipelineBuilder(pipeline.name, builderSteps = pipeline.steps)
  }

  def fromPipeline(pipelineName: String, conf: AMFGraphConfiguration): Option[TransformationPipelineBuilder] =
    conf.registry.transformationPipelines.get(pipelineName).map(pipeline => fromPipeline(pipeline))
}
