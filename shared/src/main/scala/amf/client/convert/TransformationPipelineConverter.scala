package amf.client.convert

import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.TransformationStep
import amf.client.interface.resolve.{TransformationPipeline => ClientTransformationPipeline}
import amf.client.interface.resolve.{TransformationStep => ClientTransformationStep}
import amf.client.convert.CoreClientConverters._

// not defined in CoreBaseConverter because conversion makes use of ClientList.
object TransformationPipelineConverter {
  implicit object TransformationPipelineMatcher
      extends BidirectionalMatcher[TransformationPipeline, ClientTransformationPipeline] {
    override def asClient(from: TransformationPipeline): ClientTransformationPipeline = {
      new ClientTransformationPipeline {
        override val name: String                                = from.name
        override def steps: ClientList[ClientTransformationStep] = from.steps.asClient
      }
    }
    override def asInternal(from: ClientTransformationPipeline): TransformationPipeline = {
      new TransformationPipeline {
        override val name: String                   = from.name
        override def steps: Seq[TransformationStep] = from.steps.asInternal
      }
    }
  }
}
