package amf.core.resolution.pipelines

import amf.client.remod.amfcore.resolution.PipelineName
import amf.core.errorhandling.AMFErrorHandler
import amf.core.resolution.stages.{ReferenceResolutionStage, TransformationStep}
import amf.plugins.parse.AMFGraphParsePlugin
import amf.{AmfProfile, ProfileName}

class BasicTransformationPipeline private (override val name: String) extends TransformationPipeline() {
  private def references = new ReferenceResolutionStage(keepEditingInfo = false)

  override def steps: Seq[TransformationStep] = Seq(references)
}

object BasicTransformationPipeline {
  val name: String           = PipelineName.from(AMFGraphParsePlugin.id, TransformationPipeline.DEFAULT_PIPELINE)
  def apply()                = new BasicTransformationPipeline(name)
  private[amf] def editing() = new BasicTransformationPipeline(BasicEditingTransformationPipeline.name)
}

object BasicEditingTransformationPipeline {
  val name: String = PipelineName
    .from(AMFGraphParsePlugin.id, TransformationPipeline.EDITING_PIPELINE)
  def apply(): BasicTransformationPipeline = BasicTransformationPipeline.editing()
}
