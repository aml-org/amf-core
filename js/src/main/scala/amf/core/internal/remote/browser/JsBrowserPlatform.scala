package amf.core.internal.remote.browser

import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.remote._
import amf.core.internal.remote.fs.UnsupportedFileSystem
import amf.core.internal.resource.InternalResourceLoaderAdapter
import org.mulesoft.common.io.FileSystem

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

/** */
class JsBrowserPlatform extends JsPlatform {

  /** Underlying file system for platform. */
  override val fs: FileSystem = UnsupportedFileSystem

  /** Platform out of the box [ResourceLoader]s */
  override def loaders()(implicit executionContext: ExecutionContext): Seq[ResourceLoader] =
    Seq(InternalResourceLoaderAdapter(JsBrowserHttpResourceLoader()))

  /** Return temporary directory. */
  override def tmpdir(): String = {
    // Accept in Node only
    throw new Exception(s"Unsupported tmpdir operation")
  }

  override def operativeSystem(): String = "web"
}

@JSExportAll
object JsBrowserPlatform {
  private var singleton: Option[JsBrowserPlatform] = None

  def instance(): JsBrowserPlatform = singleton match {
    case Some(p) => p
    case None =>
      singleton = Some(new JsBrowserPlatform())
      singleton.get
  }
}
