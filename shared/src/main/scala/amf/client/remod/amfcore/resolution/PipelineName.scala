package amf.client.remod.amfcore.resolution

object PipelineName {
  def from(targetMediaType: String, pipelineId: String): String = s"$targetMediaType+$pipelineId"
}
