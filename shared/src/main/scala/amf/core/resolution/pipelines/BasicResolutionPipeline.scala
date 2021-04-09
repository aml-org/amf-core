package amf.core.resolution.pipelines

import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.stages.{ReferenceResolutionStage, ResolutionStage}
import amf.{AmfProfile, ProfileName}

class BasicResolutionPipeline() extends ResolutionPipeline() {
  private def references(implicit eh: ErrorHandler) = new ReferenceResolutionStage(keepEditingInfo = false)

  override def steps(model: BaseUnit, sourceVendor: String)(
      implicit errorHandler: ErrorHandler): Seq[ResolutionStage] = Seq(references)
}
