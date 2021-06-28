package amf.core.client.platform.resource

import scala.concurrent.ExecutionContext

trait LoaderWithExecutionContext {
  def withExecutionContext(ec: ExecutionContext): ResourceLoader
}
