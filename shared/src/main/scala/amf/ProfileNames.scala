package amf

import amf.core.remote._

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

object AmfProfile     extends ProfileName(Amf.name)
object AmlProfile     extends ProfileName(Aml.name)
object UnknownProfile extends ProfileName("")

object Oas20Profile extends ProfileName(Oas20.name, OASStyle) {
  override def isOas(): Boolean = true
}

object Oas30Profile extends ProfileName(Oas30.name, OASStyle) {
  override def isOas(): Boolean = true
}

object Raml08Profile extends ProfileName(Raml08.name, RAMLStyle) {
  override def isRaml(): Boolean = true
}

object Raml10Profile extends ProfileName(Raml10.name, RAMLStyle) {
  override def isRaml(): Boolean = true
}

object AsyncProfile   extends ProfileName(AsyncApi.name, OASStyle)
object Async20Profile extends ProfileName(AsyncApi20.name, OASStyle)
object PayloadProfile extends ProfileName(Payload.name)

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
    case Amf.name             => AmfProfile
    case "OAS" | Oas20.name   => Oas20Profile // for compatibility
    case Oas30.name           => Oas30Profile
    case Raml08.name          => Raml08Profile
    case "RAML" | Raml10.name => Raml10Profile // for compatibility
    case AsyncApi.name        => AsyncProfile
    case AsyncApi20.name      => Async20Profile
    case custom               => new ProfileName(custom)
  }
}

object MessageStyle {
  def apply(name: String): MessageStyle = name match {
    case Raml10.name | Raml08.name       => RAMLStyle
    case Oas20.name | Oas30.name         => OASStyle
    case AsyncApi.name | AsyncApi20.name => OASStyle
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
