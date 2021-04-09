package amf.client.remod.amfcore.resolution

private[amf] case class PipelineInfo(targetVendor: String, pipeline: String)

object PipelineName {
  def from(targetMediaType: String, pipelineId: String): String = s"$targetMediaType+$pipelineId"
}
