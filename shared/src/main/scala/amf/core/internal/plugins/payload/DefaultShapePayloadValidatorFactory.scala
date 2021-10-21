package amf.core.internal.plugins.payload

import amf.core.client.common.validation.{SeverityLevels, StrictValidationMode, ValidationMode}
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.document.PayloadFragment
import amf.core.client.scala.model.domain.{RecursiveShape, Shape}
import amf.core.client.scala.validation.payload.{
  AMFShapePayloadValidator,
  ShapeValidationConfiguration,
  ValidatePayloadRequest
}

case class DefaultShapePayloadValidatorFactory private[amf] (config: AMFGraphConfiguration)
    extends ShapePayloadValidatorFactory {

  private lazy val plugins          = config.registry.getPluginsRegistry.payloadPlugins
  private lazy val validationConfig = ShapeValidationConfiguration(config)

  def createFor(shape: Shape, mediaType: String, mode: ValidationMode): AMFShapePayloadValidator = {
    shape match {
      case recursive: RecursiveShape =>
        recursive.fixpointTarget
          .map(target => createFor(target, mediaType, mode))
          .getOrElse(throw new Exception("Can't validate RecursiveShape with no fixpointTarget"))
      case _ =>
        plugins
          .find(_.applies(ValidatePayloadRequest(shape, mediaType, validationConfig)))
          .getOrElse(ErrorFallbackValidationPlugin(SeverityLevels.VIOLATION))
          .validator(shape, mediaType, validationConfig, mode)
    }
  }

  override def createFor(shape: Shape, fragment: PayloadFragment): AMFShapePayloadValidator = {
    createFor(shape, fragment.mediaType.value(), StrictValidationMode)
  }
}
