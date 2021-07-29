package amf.core.client.common.validation

import amf.core.internal.remote._

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("ProfileNames")
object ProfileNames {
  val AMF: ProfileName     = AmfProfile
  val OAS20: ProfileName   = Oas20Profile
  val OAS30: ProfileName   = Oas30Profile
  val RAML10: ProfileName  = Raml10Profile
  val RAML08: ProfileName  = Raml08Profile
  val ASYNC: ProfileName   = AsyncProfile
  val ASYNC20: ProfileName = Async20Profile
  val AML: ProfileName     = AmlProfile
  val PAYLOAD: ProfileName = PayloadProfile

  lazy val specProfiles: Seq[ProfileName] =
    Seq(AmfProfile, Oas20Profile, Oas30Profile, Raml08Profile, Raml10Profile, AsyncProfile, Async20Profile)
}

@JSExportAll
case class ProfileName(private[amf] val p: String, private val m: MessageStyle = AMFStyle) {
  @JSExportTopLevel("ProfileName")
  def this(profile: String) = this(profile, AMFStyle)
  def profile: String            = p
  def messageStyle: MessageStyle = m
  override def toString: String  = p
  def isOas(): Boolean           = false
  def isRaml(): Boolean          = false
}

object AmfProfile     extends ProfileName(Amf.id)
object AmlProfile     extends ProfileName(Aml.id)
object UnknownProfile extends ProfileName("")

object Oas20Profile extends ProfileName(Oas20.id, OASStyle) {
  override def isOas(): Boolean = true
}

object Oas30Profile extends ProfileName(Oas30.id, OASStyle) {
  override def isOas(): Boolean = true
}

object Raml08Profile extends ProfileName(Raml08.id, RAMLStyle) {
  override def isRaml(): Boolean = true
}

object Raml10Profile extends ProfileName(Raml10.id, RAMLStyle) {
  override def isRaml(): Boolean = true
}

object AsyncProfile   extends ProfileName(AsyncApi.id, OASStyle)
object Async20Profile extends ProfileName(AsyncApi20.id, OASStyle)
object PayloadProfile extends ProfileName(Payload.id)

object ProfileName {
  def unapply(name: String): Option[ProfileName] =
    name match {
      case AmfProfile.p     => Some(AmfProfile)
      case Oas30Profile.p   => Some(Oas30Profile)
      case Raml08Profile.p  => Some(Raml08Profile)
      case AsyncProfile.p   => Some(AsyncProfile)
      case Async20Profile.p => Some(Async20Profile)
      case _                => None
    }

  def apply(profile: String): ProfileName = profile match {
    case Amf.`id`             => AmfProfile
    case "OAS" | Oas20.`id`   => Oas20Profile // for compatibility
    case Oas30.`id`           => Oas30Profile
    case Raml08.`id`          => Raml08Profile
    case "RAML" | Raml10.`id` => Raml10Profile // for compatibility
    case AsyncApi.`id`        => AsyncProfile
    case AsyncApi20.`id`      => Async20Profile
    case custom               => new ProfileName(custom)
  }
}

object MessageStyle {
  def apply(name: String): MessageStyle = name match {
    case Raml10.`id` | Raml08.`id`       => RAMLStyle
    case Oas20.`id` | Oas30.`id`         => OASStyle
    case AsyncApi.`id` | AsyncApi20.`id` => OASStyle
    case _                               => AMFStyle
  }
}

@JSExportAll
trait MessageStyle {
  def profileName: ProfileName
}

@JSExportAll
@JSExportTopLevel("MessageStyles")
object MessageStyles {
  val RAML: MessageStyle  = RAMLStyle
  val OAS: MessageStyle   = OASStyle
  val ASYNC: MessageStyle = AsyncStyle
  val AMF: MessageStyle   = AMFStyle
}

object RAMLStyle extends MessageStyle {
  override def profileName: ProfileName = Raml10Profile
}
object OASStyle extends MessageStyle {
  override def profileName: ProfileName = Oas20Profile
}

object AsyncStyle extends MessageStyle {
  override def profileName: ProfileName = AsyncProfile
}

object AMFStyle extends MessageStyle {
  override def profileName: ProfileName = AmfProfile
}
