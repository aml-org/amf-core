package amf.core.validation

import amf.ProfileName
import amf.client.remod.amfcore.plugins.validate.{AMFValidatePlugin, ValidationOptions, ValidationResult}
import amf.core.model.document.BaseUnit

import scala.concurrent.{ExecutionContext, Future}

trait RemodValidationRunner {

  def run(unit: BaseUnit)(implicit executionContext: ExecutionContext): Future[AMFValidationReport]

  protected def emptyReport(unit: BaseUnit, profile: ProfileName): AMFValidationReport =
    AMFValidationReport.empty(unit.id, profile)
}

case class FailFastValidationRunner(plugins: Seq[AMFValidatePlugin], options: ValidationOptions)
    extends RemodValidationRunner {
  override def run(unit: BaseUnit)(implicit executionContext: ExecutionContext): Future[AMFValidationReport] = {
    val initialResult = Future.successful(ValidationResult(unit, emptyReport(unit, options.profile)))
    plugins
      .foldLeft(initialResult) { (acc, curr) =>
        acc.flatMap { validateResult =>
          failFastGuard(validateResult) {
            val oldReport = validateResult.report
            val nextReport = curr
              .validate(validateResult.unit, options)
              .map(result => ValidationResult(result.unit, oldReport.merge(result.report)))
            nextReport
          }
        }
      }
      .map(_.report)
  }

  private def failFastGuard(validateResult: ValidationResult)(
      toRun: => Future[ValidationResult]): Future[ValidationResult] = {
    if (validateResult.report.conforms) toRun else Future.successful(validateResult)
  }
}
