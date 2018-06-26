package amf.plugins.document.vocabularies.resolution.pipelines

import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.resolution.stages.{CleanReferencesStage, DeclarationsRemovalStage, ResolutionStage}
import amf.plugins.document.vocabularies.model.document.DialectInstance
import amf.plugins.document.vocabularies.resolution.stages.DialectInstanceReferencesResolutionStage
import amf.{AMFProfile, ProfileName}

class DialectInstanceResolutionPipeline(override val model: DialectInstance)
    extends ResolutionPipeline[DialectInstance] {

  override protected val steps: Seq[ResolutionStage] = Seq(
    new DialectInstanceReferencesResolutionStage(),
    new CleanReferencesStage(),
    new DeclarationsRemovalStage()
  )
  override def profileName: ProfileName = AMFProfile
}
