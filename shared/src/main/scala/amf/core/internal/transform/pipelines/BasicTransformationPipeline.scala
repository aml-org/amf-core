package amf.core.internal.transform.pipelines

import amf.core.client.common.transform.{PipelineId, PipelineName}
import amf.core.client.scala.transform.{TransformationPipeline, TransformationStep}
import amf.core.internal.transform.stages.ReferenceResolutionStage
import amf.core.internal.plugins.parse.AMFGraphParsePlugin
import amf.core.internal.remote.Amf

class BasicTransformationPipeline private (override val name: String) extends TransformationPipeline() {
  private def references = new ReferenceResolutionStage(keepEditingInfo = false)

  override def steps: Seq[TransformationStep] = Seq(references)
}

object BasicTransformationPipeline {
  val name: String           = PipelineId.Default
  def apply()                = new BasicTransformationPipeline(name)
  private[amf] def editing() = new BasicTransformationPipeline(BasicEditingTransformationPipeline.name)
}

object BasicEditingTransformationPipeline {
  val name: String                         = PipelineId.Editing
  def apply(): BasicTransformationPipeline = BasicTransformationPipeline.editing()
}
