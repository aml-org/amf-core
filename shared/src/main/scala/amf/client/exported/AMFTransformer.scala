package amf.client.exported

import amf.client.model.document.BaseUnit
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.remod.{AMFTransformer => InternalAMFTransformer}
import amf.client.convert.CoreClientConverters._

@JSExportAll
@JSExportTopLevel("AMFTransformer")
object AMFTransformer {

  def transform(unit: BaseUnit, conf: AMFGraphConfiguration): AMFResult = InternalAMFTransformer.transform(unit, conf)

  def transform(unit: BaseUnit, pipelineName: String, conf: AMFGraphConfiguration): AMFResult =
    InternalAMFTransformer.transform(unit, pipelineName, conf)

}
