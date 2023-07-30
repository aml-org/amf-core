package amf.core.render

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.{Document, Module}
import amf.core.client.scala.model.domain.{ArrayNode, ObjectNode, ScalarNode}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.convert.{BaseUnitConverter, NativeOps}
import amf.core.internal.metamodel.domain.ArrayNodeModel
import amf.core.internal.remote.Spec.AMF
import amf.core.io.FileAssertionTest
import org.scalatest.funsuite.AsyncFunSuite

import scala.concurrent.{ExecutionContext, Future}

trait AmfGraphRendererTest
    extends AsyncFunSuite
    with NativeOps
    with FileAssertionTest
    with BaseUnitConverter
    with ElementsFixture {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Test render simple document") {
    val golden = "shared/src/test/resources/parser/simple-document.jsonld"

    for {
      rendered <- Future.successful(
        AMFGraphConfiguration
          .predefined()
          .withRenderOptions(RenderOptions().withPrettyPrint.withoutFlattenedJsonLd)
          .baseUnitClient()
          .render(document, AMF.mediaType)
      )
      file   <- writeTemporaryFile(golden)(rendered)
      result <- assertDifferences(file, golden)
    } yield result
  }

  test("Test render document with urns") {
    val golden = "shared/src/test/resources/parser/urn-document.jsonld"

    for {
      rendered <- Future.successful(
        AMFGraphConfiguration
          .predefined()
          .withRenderOptions(RenderOptions().withPrettyPrint.withCompactUris.withoutFlattenedJsonLd)
          .baseUnitClient()
          .render(urnDocument, AMF.mediaType)
      )
      file   <- writeTemporaryFile(golden)(rendered)
      result <- assertDifferences(file, golden)
    } yield result
  }

}

trait ElementsFixture {
  protected val scalarNode: ScalarNode =
    ScalarNode("myValue", Some((Namespace.Xsd + "String").iri())).withId("amf://id2")
  protected val scalarNode2: ScalarNode =
    ScalarNode("myValue2", Some((Namespace.Xsd + "String").iri())).withId("amf://id4")
  protected val arrayNode: ArrayNode = ArrayNode().withId("amf://id3")
  arrayNode.addMember(scalarNode2)
  protected val objectNode: ObjectNode = ObjectNode().withId("amf://id2").addProperty("myProp1", arrayNode)

  protected val module: Module = Module().withId("amf://i21").withLocation("http://local2.com")
  protected val documentWithRef: Document =
    Document().withId("amf://id11").withLocation("http://local1.com").withRoot(true).withReferences(Seq(module))

  protected val document: Document = Document()
    .withId("amf://id1")
    .withLocation("http://local.com")
    .withEncodes(scalarNode)
    .withDeclares(Seq(arrayNode))
    .withRoot(true)

  protected val recursiveObjlvl2: ObjectNode = ObjectNode().withId("amf://id7")
  protected val arrayRecursive: ArrayNode =
    ArrayNode().withId("amf://id6").setArrayWithoutId(ArrayNodeModel.Member, Seq(recursiveObjlvl2))
  protected val recursiveObj: ObjectNode = ObjectNode().withId("amf://id5").addProperty("myProp", arrayRecursive)
  recursiveObjlvl2.addProperty("myRecursiveProp", recursiveObj)

  protected val scalarNodeWithURIValue: ScalarNode =
    ScalarNode("http://a.aml.org#/path", Some((Namespace.Xsd + "URI").iri()))
      .withId("urn:ms:acme:asset::acme:valkyr-for-humans-api:1.0.1/encodes/obj1/myProp1/scalar")
  protected val objectNodeWithUrn: ObjectNode = ObjectNode()
    .withId("urn:ms:acme:asset::acme:valkyr-for-humans-api:1.0.1/encodes/obj1/")
    .addProperty("myProp1", scalarNodeWithURIValue)

  protected val urnDocument: Document = Document()
    .withId("urn:ms:acme:asset::acme:valkyr-for-humans-api:1.0.1")
    .withLocation("http://local.com")
    .withEncodes(objectNodeWithUrn)
    .withRoot(true)
}
