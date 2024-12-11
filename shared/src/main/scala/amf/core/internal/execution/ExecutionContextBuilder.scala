package amf.core.internal.execution

import amf.core.internal.unsafe.PlatformSecrets

import java.util.concurrent.ScheduledExecutorService
import scala.concurrent.ExecutionContext

object ExecutionContextBuilder extends PlatformSecrets {

  def buildExecutionContext(scheduler: Option[ScheduledExecutorService]): ExecutionContext = scheduler match {
    case Some(s) => buildExecutionContext(s)
    case None    => getGlobalExecutionContext
  }

  def buildExecutionContext(scheduler: ScheduledExecutorService): ExecutionContext =
    ExecutionContext.fromExecutorService(scheduler)

  def getGlobalExecutionContext: ExecutionContext = platform.globalExecutionContext
}
