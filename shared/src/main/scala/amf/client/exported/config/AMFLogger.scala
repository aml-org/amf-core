package amf.client.exported.config

import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}

/**
  * AMF Logger
  */
trait AMFLogger {

  @JSExport
  def log(message: String, severity: LogSeverity, source: String)

  //  def logViolation(message: String, source: String) = log(message, ViolationSeverity, source)

}

@JSExportAll
sealed case class LogSeverity(severity: String)

@JSExportTopLevel("LogViolationSeverity")
object LogViolationSeverity extends LogSeverity("VIOLATION")
@JSExportTopLevel("LogWarningSeverity")
object LogWarningSeverity extends LogSeverity("WARNING")
@JSExportTopLevel("LogDebugSeverity")
object LogDebugSeverity extends LogSeverity("DEBUG")
@JSExportTopLevel("LogInfoSeverity")
object LogInfoSeverity extends LogSeverity("INFO")

private[amf] object MutedLogger extends AMFLogger {
  override def log(message: String, severity: LogSeverity, source: String): Unit = {}
}
