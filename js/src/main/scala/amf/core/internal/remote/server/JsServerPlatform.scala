package amf.core.internal.remote.server

import amf.core.client.platform.execution.{BaseExecutionEnvironment, DefaultExecutionEnvironment, ExecutionEnvironment}
import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.resource.InternalResourceLoaderAdapter
import amf.core.internal.remote.File.FILE_PROTOCOL
import amf.core.internal.remote._
import amf.core.internal.remote.server.JsServerPlatform.OS
import org.mulesoft.common.io.{FileSystem, Fs}

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSImport}

/**
  *
  */
class JsServerPlatform extends JsPlatform {

  /** Underlying file system for platform. */
  override val fs: FileSystem = Fs

  override def exit(code: Int): Unit = {
    js.Dynamic.global.process.exit(code)
  }

  override def loaders()(implicit executionContext: ExecutionContext): Seq[ResourceLoader] = Seq(
      InternalResourceLoaderAdapter(JsServerFileResourceLoader()),
      InternalResourceLoaderAdapter(JsServerHttpResourceLoader())
  )

  /** Return temporary directory. */
  override def tmpdir(): String = OS.tmpdir() + "/"

  override def resolvePath(uri: String): String = {
    uri match {
      case File(path) if fs.separatorChar == '/' => FILE_PROTOCOL + normalizeURL(path)
      case File(path)                            => FILE_PROTOCOL + normalizeURL(path).replace(fs.separatorChar.toString, "/")
      case HttpParts(protocol, host, path)       => protocol + host + normalizePath(withTrailingSlash(path))
      case _                                     => uri
    }
  }

  private def withTrailingSlash(path: String) = (if (!path.startsWith("/")) "/" else "") + path

  /** Return the OS (win, mac, nux). */
  override def operativeSystem(): String = {
    OS.platform() match {
      case so if so.contains("darwin") => "mac"
      case so if so.contains("win")    => "win"
      case _                           => "nux"
    }
  }
}

@JSExportAll
object JsServerPlatform {
  private var singleton: Option[JsServerPlatform] = None

  def instance(): JsServerPlatform = singleton match {
    case Some(p) => p
    case None =>
      singleton = Some(new JsServerPlatform())
      singleton.get
  }

  /** Operating System */
  @js.native
  @JSImport("os", JSImport.Namespace, "os")
  object OS extends js.Object {

    /** Returns the operating system's default directory for temporary files. */
    def tmpdir(): String = js.native

    /** Returns the operating system. */
    def platform(): String = js.native
  }
}
