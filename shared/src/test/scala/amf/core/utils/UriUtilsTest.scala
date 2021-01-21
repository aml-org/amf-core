package amf.core.utils

import org.scalatest.FunSuite
import org.scalatest.Matchers.be
import org.scalatest.Matchers._

class UriUtilsTest extends FunSuite {

  case class ResolveUrl(base: String, url: String)

  val resolveTests: List[(ResolveUrl, String)] = List(
      ResolveUrl("file://some/dir/api.raml", "./../external file.json") -> "file://some/external%20file.json",
      ResolveUrl("file://some/api.raml", "./dir with space/file.json")  -> "file://some/dir%20with%20space/file.json",
      ResolveUrl("file://", "./file.json")                              -> "file://file.json",
      ResolveUrl("file:///", "./file.json")                             -> "file:///file.json",
      ResolveUrl("file://some/dir/api.raml", "http://some.example/")    -> "http://some.example/",
      ResolveUrl("file://some/dir/api.raml", "#/some/fragment")         -> "file://some/dir/api.raml#/some/fragment",
      ResolveUrl("file://dir/other/directory/", "../file.json")         -> "file://dir/other/file.json",
      ResolveUrl("file://dir/other/directory/file.json", "")            -> "file://dir/other/directory/"
  )

  resolveTests.foreach { case (input, expected) =>
    val ResolveUrl(base, url) = input
    val result                = UriUtils.resolveRelativeTo(base, url)
    test(s"Resolved uri $expected") {
      result should be(expected)
    }
  }

  case class RemoveFileName(path: String, so: String)

  val stripFileNameTests: List[(RemoveFileName, String)] = List(
      RemoveFileName("file://some/dir/api.raml", "nux") -> "file://some/dir/",
      RemoveFileName("file://some/dir/api.raml", "mac") -> "file://some/dir/",
      RemoveFileName("file://some/dir/", "mac")         -> "file://some/dir/",
      RemoveFileName("file://some/dir", "mac")          -> "file://some/dir/",
      RemoveFileName("c:\\directory\\file.json", "win") -> "c:\\directory\\"
  )

  stripFileNameTests.foreach { case (input, expected) =>
    val result = UriUtils.stripFileName(input.path, input.so)
    test(s"Stripped file name result $expected from $input") {
      result should be(expected)
    }
  }

}
