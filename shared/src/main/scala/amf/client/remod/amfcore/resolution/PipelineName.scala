package amf.client.remod.amfcore.resolution

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel("PipelineName")
@JSExportAll
object PipelineName {
  def from(targetMediaType: String, pipelineId: String): String = s"$targetMediaType+$pipelineId"
}
