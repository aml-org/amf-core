package amf.core.client.scala.resource

import scala.concurrent.ExecutionContext

trait LoaderWithExecutionContext {
  def withExecutionContext(ec: ExecutionContext): ResourceLoader
}
