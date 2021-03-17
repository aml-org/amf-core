package amf.client.remod

import amf.client.remod.amfcore.resolution.AMFResolutionPipeline
import amf.core.model.document.BaseUnit

private[remod] object AMFResolver {

  def resolve(bu:BaseUnit, env: AMFConfiguration):AmfResult = ???

  def resolve(bu:BaseUnit, pipeline: AMFResolutionPipeline, env: AMFConfiguration):AmfResult = ???

}
