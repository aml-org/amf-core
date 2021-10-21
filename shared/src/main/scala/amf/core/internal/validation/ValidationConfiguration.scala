package amf.core.internal.validation

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.common.validation.ProfileName
import amf.core.client.scala.config.AMFEventListener
import amf.core.internal.validation.core.ValidationProfile

import scala.concurrent.ExecutionContext

object ValidationConfiguration {
  def predefined(): ValidationConfiguration = new ValidationConfiguration(AMFGraphConfiguration.predefined())
}

case class ValidationConfiguration(amfConfig: AMFGraphConfiguration) {

  val eh: AMFErrorHandler                                          = amfConfig.errorHandlerProvider.errorHandler()
  val executionContext: ExecutionContext                           = amfConfig.getExecutionContext
  val maxYamlReferences: Option[Int]                               = amfConfig.options.parsingOptions.maxYamlReferences
  val constraints: Map[ProfileName, ValidationProfile]             = amfConfig.registry.getConstraintsRules
  val listeners: Set[AMFEventListener]                             = amfConfig.listeners
  val effectiveValidations: Map[ProfileName, EffectiveValidations] = amfConfig.registry.getEffectiveValidations
}
