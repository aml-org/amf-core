package amf.core.resolution.pipelines

import amf.client.remod.amfcore.resolution.PipelineName
import amf.core.errorhandling.ErrorHandler
import amf.core.resolution.stages.{ReferenceResolutionStage, ResolutionStage}
import amf.plugins.document.graph.AMFGraphPlugin.ID
import amf.{AmfProfile, ProfileName}

class BasicResolutionPipeline private (override val name: String) extends ResolutionPipeline() {
  private def references = new ReferenceResolutionStage(keepEditingInfo = false)

  override def steps: Seq[ResolutionStage] = Seq(references)
}

object BasicResolutionPipeline {
  val name: String           = PipelineName.from(ID, ResolutionPipeline.DEFAULT_PIPELINE)
  def apply()                = new BasicResolutionPipeline(name)
  private[amf] def editing() = new BasicResolutionPipeline(BasicEditingResolutionPipeline.name)
}

object BasicEditingResolutionPipeline {
  val name: String = PipelineName
    .from(ID, ResolutionPipeline.EDITING_PIPELINE)
  def apply(): BasicResolutionPipeline = BasicResolutionPipeline.editing()
}
