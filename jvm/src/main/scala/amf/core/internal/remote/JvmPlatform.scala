package amf.core.internal.remote

import amf.core.client.platform.resource.{FileResourceLoader, HttpResourceLoader}
import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.resource.InternalResourceLoaderAdapter
import amf.core.internal.unsafe.PlatformBuilder
import org.mulesoft.common.io.{FileSystem, Fs}

import scala.concurrent.ExecutionContext

class JvmPlatform extends Platform {

  override def name = "jvm"

  /** Underlying file system for platform. */
  override val fs: FileSystem = Fs

  /** Platform out of the box [ResourceLoader]s */
  override def loaders()(implicit executionContext: ExecutionContext): Seq[ResourceLoader] = {
    Seq(
      InternalResourceLoaderAdapter(FileResourceLoader(executionContext)),
      InternalResourceLoaderAdapter(HttpResourceLoader(executionContext))
    )
  }

  /** Return temporary directory. */
  override def tmpdir(): String = System.getProperty("java.io.tmpdir")

  /** Location where the helper functions for custom validations must be retrieved */
  override def customValidationLibraryHelperLocation: String = "classpath:validations/amf_validation.js"

  override def findCharInCharSequence(stream: CharSequence)(p: Char => Boolean): Option[Char] = {
    stream.chars().filter(c => p(c.toChar)).findFirst() match {
      case optInt if optInt.isPresent => Some(optInt.getAsInt.toChar)
      case _                          => None
    }
  }

  /** encodes a complete URI, does not encodes chars like '/' */
  override def encodeURI(url: String): String = EcmaEncoder.encode(url)

  /** encodes a URI component, including chars like '/' and ':' */
  override def encodeURIComponent(url: String): String = EcmaEncoder.encode(url, fullUri = false)

  /** decode a complete URI. */
  override def decodeURI(url: String): String = EcmaEncoder.decode(url)

  /** decodes a URI component */
  override def decodeURIComponent(url: String): String = EcmaEncoder.decode(url, fullUri = false)

  private def replaceWhiteSpaces(url: String) = url.replaceAll(" ", "%20")

  /** Return the OS (win, mac, nux). */
  override def operativeSystem(): String = {
    System.getProperty("os.name").toLowerCase() match {
      case os if os.contains("win") => "win"
      case os if os.contains("mac") => "mac"
      case _                        => "nux"
    }
  }

  override val globalExecutionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}

object JvmPlatform {
  private var singleton: Option[JvmPlatform] = None

  def instance(): JvmPlatform = singleton match {
    case Some(p) => p
    case None =>
      singleton = Some(PlatformBuilder())
      singleton.get
  }
}
