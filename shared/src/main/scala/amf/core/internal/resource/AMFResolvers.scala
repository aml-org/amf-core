package amf.core.internal.resource

import amf.core.client.common.remote.Content
import amf.core.client.platform.resource.{LoaderWithExecutionContext => PlatformLoaderWithExecutionContext}
import amf.core.client.scala.config.UnitCache
import amf.core.client.scala.execution.ExecutionEnvironment
import amf.core.client.scala.resource.{LoaderWithExecutionContext, ResourceLoader}
import amf.core.internal.remote.{InternalContent, UnsupportedUrlScheme}
import amf.core.internal.unsafe.PlatformSecrets

import scala.concurrent.{ExecutionContext, Future}

/**
  * Configuration object for resolvers
  *
  * @param resourceLoaders  a list of [[ResourceLoader]] to use
  * @param unitCache        a [[UnitCache]] that stores [[amf.core.client.scala.model.document.BaseUnit]] resolved
  * @param executionEnv the [[amf.core.client.platform.execution.BaseExecutionEnvironment]] to use
  */
private[amf] case class AMFResolvers(resourceLoaders: List[ResourceLoader],
                                     unitCache: Option[UnitCache],
                                     executionEnv: ExecutionEnvironment) {

  /**
    * Add a resource loader to the [[AMFResolvers]]
    * @param resourceLoader [[ResourceLoader]] to add
    * @return an [[AMFResolvers]] with the [[ResourceLoader]] added
    */
  def withResourceLoader(resourceLoader: ResourceLoader): AMFResolvers = {
    copy(resourceLoaders = resourceLoader +: resourceLoaders)
  }

  /**
    * Add a list of [[ResourceLoader]]s
    * @param newLoaders a list of [[ResourceLoader]]s to add
    * @return an [[AMFResolvers]] with the [[ResourceLoader]]s added
    */
  def withResourceLoaders(newLoaders: List[ResourceLoader]): AMFResolvers = {
    copy(resourceLoaders = newLoaders)
  }

  /**
    * Add a [[UnitCache]] to the [[AMFResolvers]]
    * @param cache [[UnitCache]] to add
    * @return an [[AMFResolvers]] with the [[UnitCache]] added
    */
  def withCache(cache: UnitCache): AMFResolvers = {
    copy(unitCache = Some(cache))
  }

  /**
    * Add an [[ExecutionEnvironment]] to the [[AMFResolvers]]
    * @param ee the [[ExecutionEnvironment]] to add
    * @return an [[AMFResolvers]] with the [[ExecutionEnvironment]] added
    */
  def withExecutionEnvironment(ee: ExecutionEnvironment): AMFResolvers = {
    val newLoaders = adaptLoadersToNewContext(ee)
    copy(executionEnv = ee, resourceLoaders = newLoaders)
  }

  private def adaptLoadersToNewContext(ee: ExecutionEnvironment): List[ResourceLoader] = {
    resourceLoaders.map {
      case InternalResourceLoaderAdapter(a: PlatformLoaderWithExecutionContext) =>
        val adjustedLoader = a.withExecutionContext(ee.context)
        InternalResourceLoaderAdapter(adjustedLoader)(ee.context)
      case a: LoaderWithExecutionContext => a.withExecutionContext(ee.context)
      case other                         => other
    }
  }

  /**
    *
    * @param url
    * @param executionContext
    * @return
    */
  def resolveContent(url: String): Future[InternalContent] = {
    implicit val context: ExecutionContext = executionEnv.context
    val clientContent                      = loaderConcat(url, resourceLoaders.filter(_.accepts(url)))
    clientContent.map(InternalContent.withUrl(_, url))
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
    implicit val globalContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    AMFResolvers(platform.loaders()(globalContext).toList, None, ExecutionEnvironment(globalContext))
  }
}
