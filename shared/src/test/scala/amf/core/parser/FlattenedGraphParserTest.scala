package amf.core.parser

import amf.client.convert.{BaseUnitConverter, NativeOps}
import amf.client.exported.{AMFGraphConfiguration, AMFResult}
import amf.client.model.document.Document
import amf.client.model.domain.{ScalarNode => Scalar}
import amf.core.io.FileAssertionTest
import amf.core.render.ElementsFixture
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

trait FlattenedGraphParserTest
    extends AsyncFunSuite
    with NativeOps
    with FileAssertionTest
    with BaseUnitConverter
    with Matchers
    with ElementsFixture {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Test parse simple document") {
    val golden = "shared/src/test/resources/parser/simple-document.flattened.jsonld"
    /// TODO ARM use new client interfaces
    val client               = AMFGraphConfiguration.predefined().createClient()
    val f: Future[AMFResult] = client.parse("file://" + golden).asFuture

    f.map { r =>
      r.baseUnit.location shouldBe "file://" + golden
      r.baseUnit.isInstanceOf[Document] shouldBe true
      val doc = r.baseUnit.asInstanceOf[Document]
      doc.encodes.isInstanceOf[Scalar] shouldBe true
      val declared = doc.declares.asSeq.head
      declared.isInstanceOf[amf.client.model.domain.ArrayNode] shouldBe true
    }
  }

}
