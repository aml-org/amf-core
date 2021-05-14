package amf.client.convert

import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.TransformationStep
import amf.client.convert.CoreClientConverters._
import amf.client.exported.transform

// not defined in CoreBaseConverter because conversion makes use of ClientList.
object TransformationPipelineConverter {
  implicit object TransformationPipelineMatcher
      extends BidirectionalMatcher[TransformationPipeline, transform.TransformationPipeline] {
    override def asClient(from: TransformationPipeline): transform.TransformationPipeline = {
      new transform.TransformationPipeline {
        override val name: String                                    = from.name
        override def steps: ClientList[transform.TransformationStep] = from.steps.asClient
      }
    }
    override def asInternal(from: transform.TransformationPipeline): TransformationPipeline = {
      new TransformationPipeline {
        override val name: String                   = from.name
        override def steps: Seq[TransformationStep] = from.steps.asInternal
      }
    }
  }
}
