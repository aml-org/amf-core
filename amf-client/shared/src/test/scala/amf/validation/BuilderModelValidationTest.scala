package amf.validation

import amf.ProfileNames
import amf.core.model.document.{Document, Module}
import amf.core.unsafe.PlatformSecrets
import amf.core.vocabulary.Namespace.Xsd
import amf.facades.Validation
import amf.plugins.domain.shapes.models.{NodeShape, ScalarShape}
import org.scalatest.{AsyncFunSuite, Matchers}

class BuilderModelValidationTest extends AsyncFunSuite with PlatformSecrets with Matchers {

  test("Test node shape with https id for js validation functions") {

    val module = Module().withId("https://remote.com/dec/")

    val nodeShape = NodeShape()

    module.withDeclaredElement(nodeShape)
    val shape = ScalarShape().withDataType((Xsd + "string").iri())
    nodeShape.withProperty("name").withRange(shape)

    val doc = Document().withId("file://mydocument.com/")
    doc.withDeclares(Seq(nodeShape))
    doc.withReferences(Seq(module))

    for {
      validation <- Validation(platform)
      report     <- validation.validate(module, ProfileNames.RAML, ProfileNames.RAML)
    } yield {
      report.conforms should be(true)
    }
  }
}
