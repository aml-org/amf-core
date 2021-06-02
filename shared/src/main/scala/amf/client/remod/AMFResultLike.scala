package amf.client.remod

import amf.core.validation.AMFValidationReport

abstract class AMFResultLike[T](element: T, report: AMFValidationReport) {
  def conforms: Boolean = report.conforms

}
