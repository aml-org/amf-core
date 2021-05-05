package amf.client.remod.amfcore.resolution

import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.resolution.TransformationPipelineBuilder.StepsProvider
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.TransformationStep

case class TransformationPipelineBuilder private (builderName: String = "defaultBuilderName",
                                                  builderSteps: Seq[TransformationStep] = Nil) {

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

  type StepsProvider = ((ErrorHandler) => Seq[TransformationStep])

  def empty(): TransformationPipelineBuilder = new TransformationPipelineBuilder()

  def fromPipeline(pipeline: TransformationPipeline): TransformationPipelineBuilder = {
    new TransformationPipelineBuilder(builderSteps = pipeline.steps)
  }

  def fromPipeline(pipelineName: String, conf: AMFGraphConfiguration): Option[TransformationPipelineBuilder] =
    conf.registry.transformationPipelines.get(pipelineName).map(pipeline => fromPipeline(pipeline))
}
