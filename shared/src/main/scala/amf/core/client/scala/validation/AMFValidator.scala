package amf.core.client.scala.validation

import amf.core.client.common.validation._
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.validation.{ValidationInfo, ValidationOptions}
import amf.core.internal.remote.{AmlDialectSpec, Spec}
import amf.core.internal.validation.core.ValidationProfile
import amf.core.internal.validation.{EffectiveValidations, FailFastValidationRunner, ValidationConfiguration}

import scala.concurrent.Future

object VendorToProfile {

  private lazy val vendorProfileMapping = Map(
    Spec.ASYNC20 -> Async20Profile,
    Spec.RAML10  -> Raml10Profile,
    Spec.RAML08  -> Raml08Profile,
    Spec.OAS20   -> Oas20Profile,
    Spec.OAS30   -> Oas30Profile,
    Spec.OAS31   -> Oas31Profile,
    Spec.AMF     -> AmfProfile
  )

  def mapOrDefault(spec: Spec): ProfileName =
    vendorProfileMapping
      .get(spec)
      .orElse(spec match {
        case AmlDialectSpec(id) => Some(ProfileName(id))
        case _                  => None
      })
      .getOrElse(AmfProfile)
}

object AMFValidator {

  def validate(baseUnit: BaseUnit, conf: AMFGraphConfiguration): Future[AMFValidationReport] = {
    val plugins = computeApplicablePlugins(baseUnit, conf)
    val options = ValidationOptions(ValidationConfiguration(conf))
    val runner  = FailFastValidationRunner(plugins, options)
    runner.run(baseUnit)(conf.getExecutionContext)
  }

  private def computeApplicablePlugins(baseUnit: BaseUnit, conf: AMFGraphConfiguration) = {
    conf.registry.getPluginsRegistry.validatePlugins.filter(_.applies(ValidationInfo(baseUnit))).sorted
  }
}
