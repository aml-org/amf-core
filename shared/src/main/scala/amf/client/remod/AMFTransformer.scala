package amf.client.remod

import amf.client.remod.amfcore.resolution.{AMFResolutionPipeline, PipelineName}
import amf.core.model.document.BaseUnit

private[remod] object AMFTransformer {

  def transform(bu: BaseUnit, conf: AMFGraphConfiguration): AMFResult = ???

  def transform(bu: BaseUnit, pipeline: PipelineName, conf: AMFGraphConfiguration): AMFResult = ???

}
