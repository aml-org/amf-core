package amf.core.resolution.pipelines

import amf.client.remod.amfcore.resolution.PipelineName
import amf.core.errorhandling.ErrorHandler
import amf.core.resolution.stages.{ReferenceResolutionStage, TransformationStep}
import amf.plugins.document.graph.AMFGraphPlugin.ID
import amf.{AmfProfile, ProfileName}

class BasicResolutionPipeline private (override val name: String) extends TransformationPipeline() {
  private def references = new ReferenceResolutionStage(keepEditingInfo = false)

  override def steps: Seq[TransformationStep] = Seq(references)
}

object BasicResolutionPipeline {
  val name: String           = PipelineName.from(ID, TransformationPipeline.DEFAULT_PIPELINE)
  def apply()                = new BasicResolutionPipeline(name)
  private[amf] def editing() = new BasicResolutionPipeline(BasicEditingResolutionPipeline.name)
}

object BasicEditingResolutionPipeline {
  val name: String = PipelineName
    .from(ID, TransformationPipeline.EDITING_PIPELINE)
  def apply(): BasicResolutionPipeline = BasicResolutionPipeline.editing()
}
