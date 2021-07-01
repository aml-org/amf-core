package amf.core.client.common.parser

import amf.core.internal.convert.NativeOps
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.internal.unsafe.PlatformSecrets
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.ExecutionContext

trait DuplicateJsonKeysTest extends AsyncFunSuite with PlatformSecrets with NativeOps with Matchers {

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Parsed JSON with duplicate keys has several warnings") {

    val config = AMFGraphConfiguration.predefined()
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
