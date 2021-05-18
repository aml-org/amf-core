package amf.core.rdf.converter

import amf.core.errorhandling.AMFErrorHandler
import amf.plugins.features.validation.CoreValidations.UnableToConvertToScalar

trait Converter {

  protected def conversionValidation(message: String)(implicit errorHandler: AMFErrorHandler) = {
    errorHandler.violation(UnableToConvertToScalar, "", message, "")
    None
  }
}
