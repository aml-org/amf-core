package amf.client.interface.resolve

import amf.client.convert.CoreClientConverters.ClientList

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait TransformationPipeline {
  val name: String
  def steps: ClientList[TransformationStep]
}
