package amf.core.internal.remote

import org.scalatest.{FunSpec, Matchers}

class MediaTypeParserTest extends FunSpec with Matchers {

  case class Fixture(source: String, vendor: String, syntax: Option[String])
  val fixture = Seq(
      Fixture("application/raml", "application/raml", None),
      Fixture("application/raml10", "application/raml10", None),
      Fixture("application/raml+yaml", "application/raml+yaml", Some("application/yaml")),
      Fixture("application/oas20+yaml", "application/oas20+yaml", Some("application/yaml")),
      Fixture("text/plain", "text/plain", None),
      Fixture("text/plain+vnd", "text/plain+vnd", Some("text/vnd"))
  )

  fixture.foreach { f =>
    describe(s"A mediaType ${f.source}") {
      val parser = new MediaTypeParser(f.source)
      it(s"have vendor ${f.vendor}") {
        parser.getVendorExp should be(f.vendor)
      }
      f.syntax.foreach(s => {
        it(s"should have syntax $s") {
          parser.getSyntaxExp.get should be(s)
        }
      })
    }
  }

}
