package amf.core.resource.platform

import amf.core.client.platform.AMFGraphConfiguration
import amf.core.client.platform.execution.ExecutionEnvironment
import amf.core.client.platform.resource.{FileResourceLoader, HttpResourceLoader, ResourceLoader}
import amf.core.client.scala.resource.{ResourceLoader => InternalResourceLoader}
import amf.core.internal.convert.CoreClientConverters._
import org.scalatest.{FunSuite, Matchers}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class ResourceLoaderExecutionContextTest extends FunSuite with Matchers {

  test("When new execution environment is set default loaders are adjusted") {
    val config                                    = AMFGraphConfiguration.predefined()
    val defaultLoaders                            = loadersFromConfig(config)
    val executionContexts: List[ExecutionContext] = defaultLoaders.map(executionContextOfLoader)
    all(executionContexts) should be(concurrent.ExecutionContext.Implicits.global)

    val scheduler                                    = Executors.newScheduledThreadPool(5)
    val customEnv                                    = new ExecutionEnvironment(scheduler)
    val configWithCustomEnv                          = config.withExecutionEnvironment(customEnv)
    val newLoaders                                   = loadersFromConfig(configWithCustomEnv)
    val newExecutionContexts: List[ExecutionContext] = newLoaders.map(executionContextOfLoader)
    all(newExecutionContexts) should be(customEnv._internal.context)
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

  test("When new execution environment is set custom loader with context is adjusted") {
    val empty =
      AMFGraphConfiguration.empty().withResourceLoaders(List().asInstanceOf[List[InternalResourceLoader]].asClient)
    val loaderWithGlobal = new CustomECResourceLoader(concurrent.ExecutionContext.Implicits.global)

    val withInitialLoader = empty.withResourceLoader(loaderWithGlobal)
    val defaultLoader     = loadersFromConfig(withInitialLoader).head.asInstanceOf[CustomECResourceLoader]
    defaultLoader.getEc should be(concurrent.ExecutionContext.Implicits.global)

    val scheduler           = Executors.newScheduledThreadPool(5)
    val customEnv           = new ExecutionEnvironment(scheduler)
    val configWithCustomEnv = withInitialLoader.withExecutionEnvironment(customEnv)
    val modifiedLoader      = loadersFromConfig(configWithCustomEnv).head.asInstanceOf[CustomECResourceLoader]
    modifiedLoader.getEc should be(customEnv._internal.context)
  }

}
