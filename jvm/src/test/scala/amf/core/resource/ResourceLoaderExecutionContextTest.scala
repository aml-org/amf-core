package amf.core.resource

import amf.core.client.platform.AMFGraphConfiguration
import amf.core.client.platform.execution.ExecutionEnvironment
import amf.core.client.platform.resource.{FileResourceLoader, HttpResourceLoader, ResourceLoader}
import org.scalatest.{FunSuite, Matchers}
import amf.core.internal.convert.CoreClientConverters._
import amf.core.internal.resource.LoaderWithExecutionContext
import org.scalatest.Matchers.{be, _}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent
import scala.concurrent.ExecutionContext

class ResourceLoaderExecutionContextTest extends FunSuite with Matchers {

  test("When new execution environment is set default loaders are adjusted") {
    val config                                    = AMFGraphConfiguration.predefined()
    val defaultLoaders                            = loadersFromConfig(config)
    val executionContexts: List[ExecutionContext] = defaultLoaders.map(executionContextOfLoader)
    all(executionContexts) should be(concurrent.ExecutionContext.Implicits.global)

    val scheduler                                    = Executors.newScheduledThreadPool(5)
    val newConfig                                    = config.withExecutionEnvironment(new ExecutionEnvironment(scheduler))
    val newLoaders                                   = loadersFromConfig(newConfig)
    val newExecutionContexts: List[ExecutionContext] = newLoaders.map(executionContextOfLoader)
    all(newExecutionContexts) should not be (concurrent.ExecutionContext.Implicits.global)
  }

  private def loadersFromConfig(config: AMFGraphConfiguration): List[ResourceLoader] = {
    config._internal.resolvers.resourceLoaders.map(ResourceLoaderMatcher.asClient(_))
  }

  private def executionContextOfLoader(l: ResourceLoader): ExecutionContext = {
    l match {
      case HttpResourceLoader(ec) => ec
      case FileResourceLoader(ec) => ec
    }
  }

}
