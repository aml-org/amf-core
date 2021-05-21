package amf.client.exported

import amf.client.model.document.BaseUnit
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.remod.{AMFTransformer => InternalAMFTransformer}
import amf.client.convert.CoreClientConverters._

@JSExportAll
@JSExportTopLevel("AMFTransformer")
object AMFTransformer {

  def transform(unit: BaseUnit, configuration: AMFGraphConfiguration): AMFResult =
    InternalAMFTransformer.transform(unit, configuration)

  def transform(unit: BaseUnit, pipelineName: String, configuration: AMFGraphConfiguration): AMFResult =
    InternalAMFTransformer.transform(unit, pipelineName, configuration)

}
