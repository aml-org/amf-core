package amf.core.client.common.parser

import amf.core.internal.convert.{BaseUnitConverter, NativeOps}
import amf.core.client.platform.{AMFGraphConfiguration, AMFResult}
import amf.core.client.platform.model.document.Document
import amf.core.client.platform.model.domain.{ScalarNode => Scalar}
import amf.core.io.FileAssertionTest
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.render.ElementsFixture
import amf.core.internal.plugins.document.graph.parser.EmbeddedGraphParser
import org.scalatest.{AsyncFunSuite, Matchers}
import org.yaml.model.YDocument

import scala.concurrent.{ExecutionContext, Future}

trait EmbeddedGraphParserTest
    extends AsyncFunSuite
    with NativeOps
    with FileAssertionTest
    with BaseUnitConverter
    with Matchers
    with ElementsFixture {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Test parse simple document") {
    val golden = "shared/src/test/resources/parser/simple-document.expanded.jsonld"
    // TODO ARM update for new interfaces
    val client               = AMFGraphConfiguration.predefined().documentClient()
    val f: Future[AMFResult] = client.parse("file://" + golden).asFuture

    f.map { r =>
      r.baseUnit.location shouldBe "file://" + golden
      r.baseUnit.isInstanceOf[Document] shouldBe true
      val doc = r.baseUnit.asInstanceOf[Document]
      doc.encodes.isInstanceOf[Scalar] shouldBe true
      val declared = doc.declares.asSeq.head
      declared.isInstanceOf[amf.core.client.platform.model.domain.ArrayNode] shouldBe true
    }
  }

  test("Test that file with '@type' cannot be parsed by expanded parser") {

    val doc       = YDocument.parseJson("""
          |[{
          |  "id": "id",
          |  "@type": "some type"
          |}]
          |""".stripMargin)
    val parsedDoc = SyamlParsedDocument(doc)
    EmbeddedGraphParser.canParse(parsedDoc) shouldBe false
  }

}
