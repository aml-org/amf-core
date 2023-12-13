package amf.core.internal.remote.platform

import org.mulesoft.common.io.FileSystem

trait PlatformIO {

  /** Underlying file system for platform. */
  val fs: FileSystem

  def stdout(text: String): Unit = System.out.println(text)

  def stdout(e: Throwable): Unit = System.out.println(e)

  /** encodes a complete uri. Not encodes chars like / */
  def encodeURI(url: String): String

  /** decode a complete uri. */
  def decodeURI(url: String): String

  /** encodes a uri component, including chars like / and : */
  def encodeURIComponent(url: String): String

  /** decodes a uri component */
  protected def decodeURIComponent(url: String): String

  /** either decodes a uri component or returns raw url */
  def safeDecodeURIComponent(url: String): Either[String, String] =
    try {
      Right(decodeURIComponent(url))
    } catch {
      case _: Throwable => Left(url)
    }

  /** Return temporary directory. */
  def tmpdir(): String
}
