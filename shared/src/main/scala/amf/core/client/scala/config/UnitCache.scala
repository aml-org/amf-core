package amf.core.client.scala.config

import amf.core.client.scala.model.document.BaseUnit

import scala.concurrent.Future

trait UnitCache {

  /** Fetch specified reference and return associated cached reference if exists.
    *
    * @param url
    *   URL of the reference to resolve
    * @return
    *   A BaseUnit associated to the URL if present in the cache. If not present the Future will fail.
    */
  def fetch(url: String): Future[CachedReference]
}

case class CachedReference(url: String, content: BaseUnit)
