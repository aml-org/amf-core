package amf.core.parser

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.common.AsyncFunSuiteWithPlatformGlobalExecutionContext
import amf.core.internal.plugins.parse.AMFGraphParsePlugin
import org.scalatest.matchers.should.Matchers

class JsonLdContextOverrideTest extends AsyncFunSuiteWithPlatformGlobalExecutionContext with Matchers {

  private val aliases = Map("doc" -> "http://a.ml/vocabularies/document#", "data" -> "http://a.ml/vocabularies/data#")
  private val plugin  = AMFGraphParsePlugin(aliases)
  private val errorClient = AMFGraphConfiguration.predefined().baseUnitClient()
  private val validClient = AMFGraphConfiguration.predefined().withPlugin(plugin).baseUnitClient()

  test("Flattened - JSON-LD @context can be overriden by stepping on or adding new aliases") {
    val jsonld = "file://shared/src/test/resources/parser/unexpected-context.flattened.jsonld"
    for {
      errorResult <- errorClient.parse(jsonld)
      _           <- errorResult.conforms shouldBe false
      validResult <- validClient.parse(jsonld)
    } yield {
      validResult.conforms shouldBe true
    }
  }

  test("Expanded - JSON-LD @context can be overriden by stepping on or adding new aliases") {
    val jsonld = "file://shared/src/test/resources/parser/unexpected-context.expanded.jsonld"
    for {
      errorResult <- errorClient.parse(jsonld)
      _           <- errorResult.conforms shouldBe false
      validResult <- validClient.parse(jsonld)
    } yield {
      validResult.conforms shouldBe true
    }
  }
}
