package amf.core.client.platform.resource

import amf.core.client.common.remote.Content

import java.util.concurrent.CompletableFuture

trait ClientResourceLoader {

  /** Fetch specified resource and return associated content. Resource should have been previously accepted. */
  /** If the resource not exists, you should return a future failed with an ResourceNotFound exception. */
  def fetch(resource: String): CompletableFuture[Content]

  /** Accepts specified resource. */
  def accepts(resource: String): Boolean = true
}
