package amf.core.services

import amf.client.remod.amfcore.resolution.TransformationPipeline
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote.{Oas20, Oas30, Raml10}
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.plugins.features.validation.CoreValidations.ResolutionValidation

object RuntimeResolver {

  def resolve(vendor: String, unit: BaseUnit, pipelineId: String): BaseUnit =
    resolve(vendor, unit, pipelineId, unit.errorHandler())

  /**
    * interface used by amf service
    */
  def resolve(vendor: String, unit: BaseUnit, pipelineId: String, errorHandler: ErrorHandler): BaseUnit = {
    val pipelines = AMFPluginsRegistry.obtainStaticConfig().registry.transformationPipelines

    val (sourceVendor, newPipelineId) = convertFromLegacyBehaviour(vendor, pipelineId)

    val pipeline = pipelines.get(newPipelineId)

    pipeline match {
      case Some(pipeline) => pipeline.transform(unit, sourceVendor, errorHandler)
      case None =>
        errorHandler.violation(
            ResolutionValidation,
            unit.id,
            None,
            s"Cannot find domain plugin for vendor $vendor and pipeline $pipelineId to resolve unit ${unit.location()}",
            unit.position(),
            unit.location()
        )
        unit
    }
  }

  private def convertFromLegacyBehaviour(vendor: String, pipelineId: String): (String, String) = {
    (vendor, pipelineId) match {
      case (_, ResolutionPipeline.COMPATIBILITY_PIPELINE) =>
        vendor match {
          case Raml10.name => Oas20.name  -> TransformationPipeline.OAS_TO_RAML10
          case Oas20.name  => Raml10.name -> TransformationPipeline.RAML_TO_OAS20
          case Oas30.name  => Raml10.name -> TransformationPipeline.RAML_TO_OAS30
          case _           => vendor      -> pipelineId
        }
      case _ => vendor -> pipelineId
    }

  }
}
