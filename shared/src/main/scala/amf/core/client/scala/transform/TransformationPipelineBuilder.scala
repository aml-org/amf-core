package amf.core.client.scala.transform

import amf.core.client.scala.AMFGraphConfiguration

case class TransformationPipelineBuilder private (builderName: String, builderSteps: Seq[TransformationStep] = Nil) {

  def build(): TransformationPipeline = new TransformationPipeline {
    override val name: String                   = builderName
    override def steps: Seq[TransformationStep] = builderSteps
  }

  def withName(newName: String): TransformationPipelineBuilder = {
    this.copy(builderName = newName)
  }

  /** inserts stage at the end of the pipeline */
  def append(newStage: TransformationStep): TransformationPipelineBuilder = {
    this.copy(builderSteps = builderSteps :+ newStage)

  }

  /** Onserts stage at the beginning of pipeline */
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
    conf.registry.getTransformationPipelines.get(pipelineName).map(pipeline => fromPipeline(pipeline))
}
