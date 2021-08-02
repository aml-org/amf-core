package amf.core.client.scala.validation

import amf._
import amf.core.internal.validation.ValidationConfiguration
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.common.validation.{AmfProfile, Async20Profile, Oas20Profile, Oas30Profile, ProfileName, Raml08Profile, Raml10Profile}
import amf.core.internal.remote.Spec
import amf.core.internal.validation.core.ValidationProfile
import amf.core.internal.validation.FailFastValidationRunner
import amf.core.internal.plugins.validation.{ValidationInfo, ValidationOptions}
import amf.core.internal.validation.{EffectiveValidations, FailFastValidationRunner, ValidationConfiguration}

import scala.concurrent.Future

object VendorToProfile {

  private lazy val vendorProfileMapping = Map(
      Spec.ASYNC20 -> Async20Profile,
      Spec.RAML10 -> Raml10Profile,
      Spec.RAML08 -> Raml08Profile,
      Spec.OAS20 -> Oas20Profile,
      Spec.OAS30 -> Oas30Profile,
      Spec.AMF -> AmfProfile,
  )

  def mapOrDefault(vendor: Spec): ProfileName = vendorProfileMapping.getOrElse(vendor, AmfProfile)
}

object AMFValidator {

  def validate(baseUnit: BaseUnit, conf: AMFGraphConfiguration): Future[AMFValidationReport] = {
    val profileName = baseUnit.sourceSpec.map(VendorToProfile.mapOrDefault).getOrElse(AmfProfile)
    val plugins = conf.registry.plugins.validatePlugins.filter(_.applies(ValidationInfo(baseUnit, profileName))).sorted
    val constraints = computeApplicableConstraints(profileName, conf.registry.constraintsRules)
    val options = ValidationOptions(profileName, constraints, ValidationConfiguration(conf))
    val runner = FailFastValidationRunner(plugins, options)
    runner.run(baseUnit)(conf.getExecutionContext)
  }

  private def computeApplicableConstraints(profileName: ProfileName, constraints: Map[ProfileName, ValidationProfile]): EffectiveValidations = {
    val profiles = findProfileHierarchy(profileName, constraints)
    val applicable = EffectiveValidations()
    profiles.foldLeft(applicable) { (acc, curr) => acc.someEffective(curr) }
  }

  private def findProfileHierarchy(profileName: ProfileName, constraints: Map[ProfileName, ValidationProfile], seen: Set[ProfileName] = Set.empty): Seq[ValidationProfile] = {
    if (seen.contains(profileName)) return Seq.empty
    constraints.map {
      case (key, value) => key.p -> value
    }.get(profileName.p)
      .map { profile =>
        profile.baseProfile
          .map(base => findProfileHierarchy(base, constraints, seen + profile.name))
          .getOrElse(Seq.empty) ++ Seq(profile)
      }.getOrElse(Seq.empty)
  }
}
