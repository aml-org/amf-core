package amf.core.render

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.Document
import amf.core.client.scala.model.domain.{ObjectNode, SerializableAnnotation}
import amf.core.internal.annotations.{DeclaredElement, TrackedElement, VirtualElement}
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.remote.Spec.AMF
import amf.core.io.FileAssertionTest

import scala.concurrent.Future

class GovernanceModeTest extends FileAssertionTest {

  protected val allowedAnnotations: Annotations =
    Annotations() += TrackedElement(Set("")) += DeclaredElement() += VirtualElement()

  protected case class DisallowedAnnotation() extends SerializableAnnotation {
    override val name: String  = "disallowed-annotation"
    override def value: String = ""
  }

  protected val node: ObjectNode = ObjectNode(allowedAnnotations += DisallowedAnnotation())
    .withId("amf://id2")
    .withName("Node")

  test("Only allowed annotations should be rendered when using Governance Mode with Flattened JsonLd") {
    val golden = "shared/src/test/resources/render/governance-mode-flattened.jsonld"
    val documentWithNodeWithAnnotations: Document = Document()
      .withId("amf://id1")
      .withLocation("http://local.com")
      .withDeclares(Seq(node))
      .withRoot(true)

    for {
      rendered <- Future.successful(
        AMFGraphConfiguration
          .predefined()
          .withRenderOptions(RenderOptions().withPrettyPrint.withGovernanceMode)
          .baseUnitClient()
          .render(documentWithNodeWithAnnotations, AMF.mediaType)
      )
      file   <- writeTemporaryFile(golden)(rendered)
      result <- assertDifferences(file, golden)
    } yield result
  }

  test("Only allowed annotations should be rendered when using Governance Mode with Embedded JsonLd") {
    val golden = "shared/src/test/resources/render/governance-mode-expanded.jsonld"
    val documentWithNodeWithAnnotations: Document = Document()
      .withId("amf://id1")
      .withLocation("http://local.com")
      .withDeclares(Seq(node))
      .withRoot(true)

    for {
      rendered <- Future.successful(
        AMFGraphConfiguration
          .predefined()
          .withRenderOptions(RenderOptions().withPrettyPrint.withGovernanceMode.withoutFlattenedJsonLd)
          .baseUnitClient()
          .render(documentWithNodeWithAnnotations, AMF.mediaType)
      )
      file   <- writeTemporaryFile(golden)(rendered)
      result <- assertDifferences(file, golden)
    } yield result
  }
}
