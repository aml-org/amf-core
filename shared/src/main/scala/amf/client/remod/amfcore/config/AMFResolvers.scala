package amf.client.remod.amfcore.config

import amf.client.execution.BaseExecutionEnvironment
import amf.client.remote.Content
import amf.core.execution.ExecutionEnvironment
import amf.core.remote.UnsupportedUrlScheme
import amf.core.unsafe.PlatformSecrets
import amf.internal.reference.UnitCache
import amf.internal.resource.ResourceLoader

import scala.concurrent.{ExecutionContext, Future}

private[amf] case class AMFResolvers(resourceLoaders: List[ResourceLoader],
                                     val unitCache: Option[UnitCache],
                                     val executionContext: BaseExecutionEnvironment) {

  def withResourceLoader(resourceLoader: ResourceLoader): AMFResolvers = {
    copy(resourceLoaders = resourceLoader +: resourceLoaders)
  }

  def withResourceLoaders(newLoaders: List[ResourceLoader]): AMFResolvers = {
    copy(resourceLoaders = newLoaders)
  }

  def withCache(cache: UnitCache): AMFResolvers = {
    copy(unitCache = Some(cache))
  }

  def resolveContent(url: String)(implicit executionContext: ExecutionContext): Future[Content] = {
    loaderConcat(url, resourceLoaders.filter(_.accepts(url)))
  }

  private def loaderConcat(url: String, loaders: Seq[ResourceLoader])(
      implicit executionContext: ExecutionContext): Future[Content] = loaders.toList match {
    case Nil         => throw new UnsupportedUrlScheme(url)
    case head :: Nil => head.fetch(url)
    case head :: tail =>
      head.fetch(url).recoverWith {
        case _ => loaderConcat(url, tail)
      }
  }

}

object AMFResolvers extends PlatformSecrets {

  /**
    * Predefined amf resolvers with:
    *  - Default resource loaders by platform
    *  - Without units cache
    * @return
    */
  def predefined() = {
    // TODO: user execution environment?
    AMFResolvers(platform.loaders()((platform.defaultExecutionEnvironment).executionContext).toList,
                 None,
                 (platform.defaultExecutionEnvironment))
  }
}
