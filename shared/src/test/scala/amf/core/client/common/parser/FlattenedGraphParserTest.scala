package amf.core.client.common.parser

import amf.core.client.platform.model.document.Document
import amf.core.client.platform.model.domain.{ScalarNode => Scalar}
import amf.core.client.platform.{AMFGraphConfiguration, AMFResult}
import amf.core.client.scala.AMFParseResult
import amf.core.client.scala.model.domain.{AmfObject, DomainElement}
import amf.core.client.scala.vocabulary.Namespace.Data
import amf.core.client.scala.vocabulary.ValueType
import amf.core.internal.convert.{BaseUnitConverter, NativeOps}
import amf.core.internal.metamodel.{Field, ModelDefaultBuilder, Obj, Type}
import amf.core.internal.metamodel.domain.{DomainElementModel, ModelDoc}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.io.FileAssertionTest
import amf.core.render.ElementsFixture
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

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

    val client               = AMFGraphConfiguration.predefined().baseUnitClient()
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

}
