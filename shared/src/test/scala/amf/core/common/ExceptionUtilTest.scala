package amf.core.common

import amf.core.client.common.{AmfExceptionCode, ExceptionUtil}
import amf.core.client.platform.resource.ResourceNotFound
import amf.core.internal.remote.NetworkError
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ExceptionUtilTest extends AnyFunSuite with Matchers {

  test("Test ExceptionUtil ResourceNotFound") {
    ExceptionUtil.isExceptionType(new ResourceNotFound(""), AmfExceptionCode.ResourceNotFound) shouldBe true
  }

  test("Test ExceptionUtil NetworkError") {
    ExceptionUtil.isExceptionType(NetworkError(new Throwable()), AmfExceptionCode.NetworkError) shouldBe true
  }

}
