package amf.client.remod

import amf.client.remod.amfcore.resolution.{AMFResolutionPipeline, PipelineName}
import amf.core.model.document.BaseUnit

private[remod] object AMFTransformer {

  def transform(bu: BaseUnit, conf: AMFConfiguration): AMFResult = ???

  def transform(bu: BaseUnit, pipeline: PipelineName, conf: AMFConfiguration): AMFResult = ???

}
