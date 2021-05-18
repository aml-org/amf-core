package amf.client.resolve

import amf.client.convert.CoreClientConverters._
import amf.client.model.document.BaseUnit
import amf.client.resolve.ClientErrorHandlerConverter._
import amf.client.validate.ValidationResult
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.services.RuntimeResolver
import amf.core.validation.AMFValidationResult

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel("Resolver")
@JSExportAll
class Resolver(vendor: String) {

  def resolve(unit: BaseUnit): BaseUnit = resolve(unit, TransformationPipeline.DEFAULT_PIPELINE)

  def resolve(unit: BaseUnit, pipeline: String): BaseUnit =
    RuntimeResolver.resolve(vendor, unit, pipeline)

  def resolve(unit: BaseUnit, pipeline: String, errorHandler: ClientErrorHandler): BaseUnit =
    RuntimeResolver.resolve(vendor, unit, pipeline, errorHandler)
}

@JSExportAll
trait ClientErrorHandler {

  def getResults: ClientList[ValidationResult]

  def report(result: ValidationResult): Unit
}
