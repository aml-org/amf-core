package amf.core.client.common.parser

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.common.AsyncFunSuiteWithPlatformGlobalExecutionContext
import amf.core.internal.convert.NativeOps
import amf.core.internal.plugins.parse.ExternalFragmentDomainFallback
import org.scalatest.matchers.should.Matchers

trait DuplicateJsonKeysTest extends AsyncFunSuiteWithPlatformGlobalExecutionContext with NativeOps with Matchers {

  test("Parsed JSON with duplicate keys has several warnings") {

    val config = AMFGraphConfiguration.predefined().withFallback(ExternalFragmentDomainFallback(false))
    val url    = "file://shared/src/test/resources/parser/duplicate-key.json"
    config.baseUnitClient().parse(url).map { r =>
      val errors = r.results
      errors.size should be(4)
      val allAreDuplicateKeyWarnings =
        errors.forall(r => r.completeMessage.contains("Duplicate key") && r.severityLevel.contains("Warning"))
      allAreDuplicateKeyWarnings shouldBe true
    }
  }
}
