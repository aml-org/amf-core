package amf.core.internal.remote

import amf.core.client.common.AmfExceptionCode
import amf.core.client.common.remote.Content
import amf.core.client.platform.model.AmfObjectWrapper
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfObject, DomainElement}
import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.metamodel.Obj
import amf.core.internal.remote.platform.{PlatformIO, PlatformResourceOps, PlatformWrapperOps}
import org.mulesoft.common.io.{AsyncFile, FileSystem, SyncFile}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object Platform {
  def base(url: String): Option[String] = Some(url.substring(0, url.lastIndexOf('/')))
}

trait Platform extends PlatformWrapperOps with PlatformIO with PlatformResourceOps {

  val globalExecutionContext: ExecutionContext

  def name: String = "gen"

  def findCharInCharSequence(s: CharSequence)(p: Char => Boolean): Option[Char]

  /** Return the OS (win, mac, nux). */
  def operativeSystem(): String
}
