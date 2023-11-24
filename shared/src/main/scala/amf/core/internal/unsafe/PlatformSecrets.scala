package amf.core.internal.unsafe

import amf.core.internal.remote.Platform

import scala.concurrent.ExecutionContext

trait PlatformSecrets {
  val platform: Platform = PlatformBuilder()
}

// Not use along with AsyncFunSuite. Use AsyncFunSuiteWithPlatformGlobalExecutionContext
trait PlatformSecretsWithImplicitGlobalExecutionContext extends PlatformSecrets {
  implicit val executionContext: ExecutionContext = platform.globalExecutionContext
}
