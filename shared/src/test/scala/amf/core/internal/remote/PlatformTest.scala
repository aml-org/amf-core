package amf.core.internal.remote

import amf.core.client.common.remote.Content
import amf.core.internal.remote.Mimes.`application/yaml`
import amf.core.internal.resource.AMFResolvers
import amf.core.internal.unsafe.PlatformSecrets
import org.mulesoft.common.test.ListAssertions
import org.scalatest.Assertion
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

class PlatformTest extends AsyncFunSuite with Matchers with ListAssertions with PlatformSecrets {

  override implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  test("File") {
    AMFResolvers.predefined().resolveContent("file://shared/src/test/resources/input.yaml") map {
      case InternalContent(content, _, mime) =>
        mime should contain(`application/yaml`)

        content.toString should be
        """|a: 1
                     |b: !include includes/include1.yaml
                     |c:
                     |  - 2
                     |d: !include includes/include2.yaml""".stripMargin

        content.sourceName.replace(platform.fs.separatorChar.toString, "/") should be(
            "shared/src/test/resources/input.yaml")
    }
  }

  ignore("http") {
    val path = "http://amf.us-2.evennode.com/input.yaml"

    AMFResolvers
      .predefined()
      .resolveContent(path)
      .map(stream => {
        val content = stream.toString

        assert(
            content equals
              """|a: 1
                 |b: !include http://amf.us-2.evennode.com/include1.yaml/4000
                 |c:
                 |  - 2
                 |d: !include http://amf.us-2.evennode.com/include2.yaml/4000""".stripMargin)
      })
  }

  ignore("Write") {
    val path = "file:///tmp/" + new Date().getTime
    platform.write(path, "{\n\"name\" : \"Jason Bourne\"\n}").map[Assertion](unit => path should not be null)
  }
}
