package amf.core.client.scala.validation

import amf.core.client.common.validation.SeverityLevels.{INFO, VIOLATION, WARNING}
import amf.core.internal.annotations.LexicalInformation
import org.mulesoft.common.client.lexical.{Position, PositionRange}
import org.mulesoft.common.io.Output
import org.mulesoft.common.io.Output._

import java.io.StringWriter

object AMFValidationReportPrinter {

  val DefaultMax = 30

  def print(report: AMFValidationReport, max: Int): String = {
    val output = new StringWriter
    print(report, output, max)
    output.toString
  }

  def print[W: Output](report: AMFValidationReport, writer: W, max: Int = DefaultMax): Unit = {

    val AMFValidationReport(model, profile, _) = report

    writer.append(s"ModelId: $model\n")
    writer.append(s"Profile: $profile\n")
    printConformance(report, writer, max)
  }

  def printConformance[W: Output](conformance: ReportConformance, writer: W, max: Int): Unit = {
    writer.append(s"Conforms: ${conformance.conforms}\n")
    writer.append(s"Number of results: ${conformance.results.length}\n")

    val groupedValidations =
      conformance.results.take(max).sortWith((c1, c2) => c1.compare(c2) < 0).groupBy(_.severityLevel)

    groupedValidations.get(VIOLATION).foreach(violations => appendValidations(VIOLATION, violations, writer))
    groupedValidations.get(WARNING).foreach(violations => appendValidations(WARNING, violations, writer))
    groupedValidations.get(INFO).foreach(violations => appendValidations(INFO, violations, writer))
  }

  def print(report: AMFValidationResult): String = {
    val output = new StringWriter
    print(report, output)
    output.toString
  }

  def print[W: Output](result: AMFValidationResult, writer: W): Unit = {

    val AMFValidationResult(message, severityLevel, _targetNode, targetProperty, validationId, position, location, _) =
      result

    writer.append(s"- Constraint: $validationId\n")
    writer.append(s"  Message: $message\n")
    writer.append(s"  Severity: $severityLevel\n")
    writer.append(s"  Target: ${result.targetNode}\n")
    writer.append(s"  Property: ${targetProperty.getOrElse("")}\n")
    writer.append(s"  Range: ${serialize(position)}\n")
    writer.append(s"  Location: ${location.getOrElse("")}\n")
  }

  private def serialize(maybeLexical: Option[LexicalInformation]): String = {
    maybeLexical
      .map { lexical =>
        val PositionRange(start, end)        = lexical.range
        val Position(startLine, startCol, _) = start
        val Position(endLine, endCol, _)     = end
        s"[($startLine,$startCol)-($endLine,$endCol)]"
      }
      .getOrElse("")
  }

  private def appendValidations[W: Output](level: String, validations: Seq[AMFValidationResult], writer: W): Unit = {
    writer.append(s"\nLevel: $level\n")
    validations.foreach { result =>
      writer.append("\n")
      print(result, writer)
    }
  }
}
