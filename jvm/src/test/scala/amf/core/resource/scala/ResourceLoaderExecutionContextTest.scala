package amf.core.resource.scala

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.execution.ExecutionEnvironment
import org.scalatest.{FunSuite, Matchers}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class ResourceLoaderExecutionContextTest extends FunSuite with Matchers {

  test("When new execution environment is set custom loader with context is adjusted") {
    val defaultConf = AMFGraphConfiguration
      .empty()
      .withResourceLoaders(List(CustomECResourceLoader(concurrent.ExecutionContext.Implicits.global)))

    val defaultLoader = defaultConf.resolvers.resourceLoaders.head.asInstanceOf[CustomECResourceLoader]
    defaultLoader.ec should be(concurrent.ExecutionContext.Implicits.global)

    val customEnv           = new ExecutionEnvironment(ExecutionContext.fromExecutorService(Executors.newScheduledThreadPool(5)))
    val configWithCustomEnv = defaultConf.withExecutionEnvironment(customEnv)
    val modifiedLoader      = configWithCustomEnv.resolvers.resourceLoaders.head.asInstanceOf[CustomECResourceLoader]
    modifiedLoader.ec should be(customEnv.context)
  }

}
