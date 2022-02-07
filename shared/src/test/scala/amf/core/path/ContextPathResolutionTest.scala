package amf.core.path

import amf.core.internal.remote.Context
import amf.core.internal.unsafe.PlatformSecrets
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ContextPathResolutionTest extends AnyFunSuite with PlatformSecrets with Matchers {

  test("file:// and /exchange_modules concat result in file://exchange_modules") {
    val context = Context(platform)
      .update("file://api.raml")
      .update("/exchange_modules/aReallyCoolModule/datatype.raml")
    context.current shouldBe "file://exchange_modules/aReallyCoolModule/datatype.raml"
    context.current should not be "file:///exchange_modules/aReallyCoolModule/datatype.raml"

  }

  test("Context can resolve one relative directory up") {
    val context = Context(platform)
      .update("file://datatypes/anotherFolder/api.raml")
      .update("../exchange_modules/aReallyCoolModule/datatype.raml")
    context.current shouldBe "file://datatypes/exchange_modules/aReallyCoolModule/datatype.raml"
  }
}
