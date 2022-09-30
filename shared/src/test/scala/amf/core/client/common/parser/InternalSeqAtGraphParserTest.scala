package amf.core.client.common.parser

import amf.core.client.scala.model.document.Document
import amf.core.client.scala.{AMFGraphConfiguration, AMFParseResult}
import amf.core.client.scala.model.domain.{AmfObject, DomainElement}
import amf.core.client.scala.vocabulary.Namespace.Data
import amf.core.client.scala.vocabulary.ValueType
import amf.core.internal.convert.{BaseUnitConverter, NativeOps}
import amf.core.internal.metamodel.domain.DomainElementModel
import amf.core.internal.metamodel.{Field, ModelDefaultBuilder, Obj, Type}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.io.FileAssertionTest
import amf.core.render.ElementsFixture
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class InternalSeqAtGraphParserTest()
    extends AsyncFunSuite
    with FileAssertionTest
    with BaseUnitConverter
    with Matchers
    with ElementsFixture {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  private def parseDynamic(source: String, seqType: Type) = {
    val golden = s"shared/src/test/resources/parser/$source"
    val entity: ModelDefaultBuilder = new ModelDefaultBuilder {
      val obj = this

      override def modelInstance: AmfObject = new DomainElement {
        override def meta: Obj = obj

        /** Set of fields composing object. */
        override val fields: Fields = Fields()

        /** Value , path + field value that is used to compose the id when the object its adopted */
        override private[amf] def componentId = "fake"

        /** Set of annotations for element. */
        override val annotations: Annotations = Annotations()
      }

      override val `type`: List[ValueType] = List(Data + "myNode") ++ DomainElementModel.`type`

      override def fields: List[Field] = List(Field(Type.Array(seqType), Data + "myseq"))
    }

    val configuration = AMFGraphConfiguration.predefined().withEntities(Map(entity.`type`.head.iri() -> entity))

    val client = configuration.baseUnitClient()
    for {
      flattened <- client.parse("file://" + golden + ".flattened.jsonld")
      expanded  <- client.parse("file://" + golden + ".expanded.jsonld")
    } yield (flattened, expanded)

  }

  test("Test parse array of numbers") {
    parseDynamic("array-of-numbers", Type.Int).map { r =>
      assertResult(r._1)
      assertResult(r._2)
    }
  }

  test("Test parse array of any") {
    parseDynamic("array-of-any", Type.Any).map { r =>
      assertResult(r._1)
      assertResult(r._2)

    }
  }

  def assertResult(r: AMFParseResult) = {
    r.baseUnit.isInstanceOf[Document] shouldBe true
    val doc = r.baseUnit.asInstanceOf[Document]
    val seq = doc.encodes.graph.scalarByProperty((Data + "myseq").iri())
    seq.size shouldBe 2
  }
}
