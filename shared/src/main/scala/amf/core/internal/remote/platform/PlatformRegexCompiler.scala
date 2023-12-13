package amf.core.internal.remote.platform

trait PlatformRegexCompiler {

  def regex(regex: String): PlatformRegex
}

trait PlatformRegex {
  def apply(regex: String): PlatformRegex
  def test(value: String): Boolean
  def findFirstIn(value: String): Option[String]
}
