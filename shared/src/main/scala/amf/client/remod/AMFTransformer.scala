package amf.client.remod

import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.TransformationPipeline

private[remod] object AMFTransformer {

  def transform(bu: BaseUnit, conf: AMFGraphConfiguration): AMFResult = ???

  // PipelineName object should be used to form name given certain vendor and pipeline
  def transform(bu: BaseUnit, pipelineName: String, conf: AMFGraphConfiguration): AMFResult = ???

}
