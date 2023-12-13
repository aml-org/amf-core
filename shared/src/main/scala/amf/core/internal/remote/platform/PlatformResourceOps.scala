package amf.core.internal.remote.platform

import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.remote.UnsupportedUrlScheme

import scala.concurrent.{ExecutionContext, Future}

trait PlatformResourceOps {
  private def loaderConcat(url: String, loaders: Seq[ResourceLoader])(implicit
      executionContext: ExecutionContext
  ): Future[Content] = loaders.toList match {
    case Nil         => throw new UnsupportedUrlScheme(url)
    case head :: Nil => head.fetch(url)
    case head :: tail =>
      head.fetch(url).recoverWith { case _ =>
        loaderConcat(url, tail)
      }
  }

  /** Resolve remote url. */
  def fetchContent(url: String, configuration: AMFGraphConfiguration)(implicit
      executionContext: ExecutionContext
  ): Future[Content] =
    loaderConcat(url, configuration.getResourceLoaders.filter(_.accepts(url)))

  /** Platform out of the box [ResourceLoader]s */
  def loaders()(implicit executionContext: ExecutionContext): Seq[ResourceLoader]
}
