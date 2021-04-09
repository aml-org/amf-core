package amf.core.resolution.pipelines

import amf.core.errorhandling.ErrorHandler
import amf.core.resolution.stages.{ReferenceResolutionStage, ResolutionStage}
import amf.{AmfProfile, ProfileName}

class BasicResolutionPipeline() extends ResolutionPipeline() {
  private def references(implicit eh: ErrorHandler) = new ReferenceResolutionStage(keepEditingInfo = false)

  override def steps(implicit eh: ErrorHandler): Seq[ResolutionStage] = Seq(references)
}
