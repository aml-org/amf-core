package amf.core.uri

import amf.core.internal.utils.UriUtils
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class UriNormalizationTest extends AnyFunSuite with Matchers {

  test("URI normalization without prefix") {
    val ref = "../folder3/response.json"
    val currentLoc = "/folder1/folder2/api.json"
    val expected = "/folder1/folder3/response.json"

    val resolved = UriUtils.resolveRelativeTo(currentLoc, ref)

    assert(resolved == expected)
  }

  test("URI normalization with prefix") {
    val ref = "../folder3/response.json"
    val currentLoc = "file:/folder1/folder2/api.json"
    val expected = "file:///folder1/folder3/response.json"

    val resolved = UriUtils.resolveRelativeTo(currentLoc, ref)

    assert(resolved == expected)
  }

  test("URI normalization with 2 prefixes") {
    val ref = "../folder3/response.json"
    val currentLoc = "jar:file:/folder1/folder2/api.json"
    val expected = "jar:file:/folder1/folder3/response.json"

    val resolved = UriUtils.resolveRelativeTo(currentLoc, ref)

    assert(resolved == expected)
  }
}
