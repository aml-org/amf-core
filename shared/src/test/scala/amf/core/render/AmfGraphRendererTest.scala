package amf.core.render

import amf.Core
import amf.client.convert.{BaseUnitConverter, NativeOps}
import amf.client.exported.AMFGraphConfiguration
import amf.client.exported.config.RenderOptions
import amf.core.io.FileAssertionTest
import amf.core.metamodel.domain.ArrayNodeModel
import amf.core.model.document.{Document, Module}
import amf.core.model.domain.{ArrayNode, ObjectNode, ScalarNode}
import amf.core.remote.Vendor.AMF
import amf.core.vocabulary.Namespace
import org.scalatest.AsyncFunSuite

import scala.concurrent.ExecutionContext

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
      _ <- Core.init().asFuture // TODO ARM: when wrapper stop being registered, remove this
      rendered <- AMFGraphConfiguration
        .predefined()
        .withRenderOptions(new RenderOptions().withPrettyPrint)
        .createClient()
        .render(BaseUnitMatcher.asClient(document), AMF.mediaType)
        .asFuture
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
}
