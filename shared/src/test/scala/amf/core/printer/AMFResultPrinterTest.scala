package amf.core.printer

import amf.core.client.common.validation.SeverityLevels
import amf.core.client.scala.AMFResult
import amf.core.client.scala.model.document.Document
import amf.core.client.scala.validation.AMFValidationResult
import amf.core.io.FileAssertionTest

class AMFResultPrinterTest extends FileAssertionTest {

  val goldenPath = "shared/src/test/resources/result/result.txt"

  test("AMFResult.toString returns a printed report of it's results and if the unit conforms") {
    val resultAsString = buildResult().toString
    writeTemporaryFile(goldenPath)(resultAsString).flatMap(file => assertDifferences(file, goldenPath))
  }

  private def buildResult(): AMFResult = {
    val unit = Document().withId("baseDoc")
    val results = List(
      AMFValidationResult("aMessage1", SeverityLevels.VIOLATION, "a", None, "b", None, None, None),
      AMFValidationResult("aMessage2", SeverityLevels.WARNING, "c", None, "d", None, None, None),
      AMFValidationResult("aMessage3", SeverityLevels.INFO, "f", None, "g", None, None, None),
      AMFValidationResult("aMessage4", SeverityLevels.WARNING, "h", None, "i", None, None, None)
    )
    AMFResult(unit, results)
  }
}
