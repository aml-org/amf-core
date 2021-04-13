package amf.client.plugins

import amf.client.remod.amfcore.plugins.validate.AMFValidatePlugin
import amf.core.remote.Platform
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.ValidationProfile

trait AMFValidationPlugin extends AMFPlugin with PlatformSecrets {

  /**
    * Validation profiles supported by this plugin by default
    */
  def domainValidationProfiles: Seq[ValidationProfile]

  protected[amf] def getRemodValidatePlugins(): Seq[AMFValidatePlugin]
}
