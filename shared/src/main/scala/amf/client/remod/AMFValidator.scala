package amf.client.remod

import amf.{AmfProfile, AmlProfile, Async20Profile, AsyncProfile, Oas20Profile, Oas30Profile, ProfileName, Raml08Profile, Raml10Profile}
import amf.client.remod.amfcore.plugins.validate.{ValidationConfiguration, ValidationOptions}
import amf.core.model.document.BaseUnit
import amf.core.remote.Vendor
import amf.core.validation.core.ValidationProfile
import amf.core.validation.{AMFValidationReport, EffectiveValidations, FailFastValidationRunner}

import scala.concurrent.{ExecutionContext, Future}

object VendorToProfile {

  private lazy val vendorProfileMapping = Map(
      Vendor.ASYNC -> Async20Profile,
      Vendor.RAML10 -> Raml10Profile,
      Vendor.RAML08 -> Raml08Profile,
      Vendor.OAS20 -> Oas20Profile,
      Vendor.OAS30 -> Oas30Profile,
      Vendor.AMF -> AmfProfile,
  )

  def mapOrDefault(vendor: Vendor): ProfileName = vendorProfileMapping.getOrElse(vendor, AmfProfile)
}

object AMFValidator {
  def validate(bu: BaseUnit, conf: AMFGraphConfiguration): Future[AMFValidationReport] = {
    val guessedVendor = bu.sourceVendor.map(v => VendorToProfile.mapOrDefault(v)).getOrElse(AmfProfile)
    validate(bu, guessedVendor, conf)
  }
  def validate(bu: BaseUnit, profileName: ProfileName, conf: AMFGraphConfiguration): Future[AMFValidationReport] = {
    val plugins = conf.registry.plugins.validatePlugins
    val constraints = computeApplicableConstraints(profileName, conf.registry.constraintsRules)
    val options = ValidationOptions(profileName, constraints, ValidationConfiguration(conf))
    val runner = FailFastValidationRunner(plugins, options)
    runner.run(bu)(conf.getExecutionContext)
  }

  private def computeApplicableConstraints(profileName: ProfileName, constraints: Map[ProfileName, ValidationProfile]): EffectiveValidations = {
    val profiles = findProfileHierarchy(profileName, constraints)
    val applicable = EffectiveValidations()
    profiles.foldLeft(applicable) { (acc, curr) => acc.someEffective(curr) }
  }

  private def findProfileHierarchy(profileName: ProfileName, constraints: Map[ProfileName, ValidationProfile], seen: Set[ProfileName] = Set.empty): Seq[ValidationProfile] = {
    if (seen.contains(profileName)) return Seq.empty
    constraints.get(profileName)
      .map { profile =>
        profile.baseProfile
          .map(base => findProfileHierarchy(base, constraints, seen + profile.name))
          .getOrElse(Seq.empty) ++ Seq(profile)
      }.getOrElse(Seq.empty)
  }
}
