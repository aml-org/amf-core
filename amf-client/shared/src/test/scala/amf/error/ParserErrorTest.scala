package amf.error

import amf.client.parse.DefaultErrorHandler
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.AMFValidationResult
import amf.facades.Validation
import org.scalatest.{Assertion, AsyncFunSuite, Matchers, Succeeded}

import scala.concurrent.{ExecutionContext, Future}

trait ParserErrorTest extends AsyncFunSuite with PlatformSecrets with Matchers {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  protected val basePath: String

  protected def build(eh: AMFErrorHandler, file: String): Future[BaseUnit]

  protected def validate(file: String, fixture: AMFValidationResult => Unit*): Future[Assertion] = {
    validateWithUnit(file, (_: BaseUnit) => Unit, fixture)
  }

  protected def validateWithUnit(file: String,
                                 unitAssertion: BaseUnit => Unit,
                                 fixture: Seq[AMFValidationResult => Unit]): Future[Assertion] = {
    val eh = DefaultErrorHandler()
    Validation(platform).flatMap { _ =>
      build(eh, basePath + file)
        .map { u =>
          unitAssertion(u)
          val report = eh.getResults
          if (report.size != fixture.size) {
            report.foreach(println)
            fail(s"Expected results has length of ${fixture.size} while actual results are ${report.size}")
          }
          fixture.zip(report).foreach {
            case (fn, result) => fn(result)
          }
          Succeeded
        }
    }
  }
}
