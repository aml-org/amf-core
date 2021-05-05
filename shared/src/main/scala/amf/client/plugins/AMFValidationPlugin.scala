package amf.client.plugins

import amf.ProfileName
import amf.client.execution.BaseExecutionEnvironment
import amf.client.remod.amfcore.plugins.validate.AMFValidatePlugin
import amf.core.model.document.BaseUnit
import amf.core.remote.Platform
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.ValidationProfile
import amf.core.validation.{AMFValidationReport, EffectiveValidations}
import amf.internal.environment.Environment

import scala.concurrent.{ExecutionContext, Future}

trait AMFValidationPlugin extends AMFPlugin with PlatformSecrets {

  /**
    * Validation profiles supported by this plugin by default
    */
  def domainValidationProfiles(platform: Platform): Map[String, () => ValidationProfile]

  protected[amf] def getRemodValidatePlugins(): Seq[AMFValidatePlugin]
}
