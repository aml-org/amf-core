package amf.client.remod.amfcore.config

private[amf] class AMFLogger {

  private def log(message: String, severity: LogSeverity, source: String) = {}

  def logViolation(message: String, source: String) = log(message, ViolationSeverity, source)

  //....
}

private[remod] object MutedLogger extends AMFLogger {}
private[remod] sealed case class LogSeverity(severity: String)

object ViolationSeverity extends LogSeverity("VIOLATION")
object WarningSeverity   extends LogSeverity("WARNING")
object DebugSeverity     extends LogSeverity("DEBUG")
object InfoSeverity      extends LogSeverity("INFO")
