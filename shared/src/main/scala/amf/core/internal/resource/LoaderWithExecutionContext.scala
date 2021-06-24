package amf.core.internal.resource

import amf.core.client.platform.resource.ResourceLoader

import scala.concurrent.ExecutionContext

trait LoaderWithExecutionContext {
  def withNewContext(ec: ExecutionContext): ResourceLoader
}
