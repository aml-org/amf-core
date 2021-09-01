package amf.core.client.scala.validation

import amf.core.client.common.validation.SeverityLevels.{INFO, VIOLATION, WARNING}
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

    writer.append(s"Model: $model\n")
    writer.append(s"Profile: $profile\n")
    printConformance(report, writer, max)
  }

  def printConformance[W: Output](conformance: ReportConformance, writer: W, max: Int): Unit = {
    writer.append(s"Conforms? ${conformance.conforms}\n")
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

    val AMFValidationResult(message, severityLevel, targetNode, targetProperty, validationId, position, location, _) =
      result

    writer.append(s"- Source: $validationId\n")
    writer.append(s"  Message: $message\n")
    writer.append(s"  Level: $severityLevel\n")
    writer.append(s"  Target: $targetNode\n")
    writer.append(s"  Property: ${targetProperty.getOrElse("")}\n")
    writer.append(s"  Position: $position\n")
    writer.append(s"  Location: ${location.getOrElse("")}\n")
  }

  private def appendValidations[W: Output](level: String, validations: Seq[AMFValidationResult], writer: W): Unit = {
    writer.append(s"\nLevel: $level\n")
    validations.foreach { result =>
      writer.append("\n")
      print(result, writer)
    }
  }
}
