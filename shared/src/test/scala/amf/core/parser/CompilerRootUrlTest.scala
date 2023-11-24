package amf.core.parser

import amf.core.client.common.remote.Content
import amf.core.client.common.{HighPriority, PluginPriority}
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.adoption.{DefaultIdMaker, IdAdopter, IdAdopterProvider, IdMaker}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.{BaseUnit, Document, Module}
import amf.core.client.scala.model.domain.{AmfObject, ScalarNode}
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler, SimpleReferenceHandler, SyamlParsedDocument}
import amf.core.client.scala.resource.ResourceLoader
import amf.core.client.scala.vocabulary.Namespace.XsdTypes
import amf.core.common.AsyncFunSuiteWithPlatformGlobalExecutionContext
import amf.core.internal.adoption.BaseUnitFieldAdoptionOrdering
import amf.core.internal.metamodel.document.DocumentModel
import amf.core.internal.parser.domain.FieldEntry
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder, Root}
import amf.core.internal.remote.Spec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class CompilerRootUrlTest extends AsyncFunSuiteWithPlatformGlobalExecutionContext with Matchers {

  class CustomContentUrlResourceLoader(customUrl: String) extends ResourceLoader {
    override def fetch(resource: String): Future[Content] = Future.successful(
      new Content(
        """
          |{
          |   "a": 5
          |}""".stripMargin,
        customUrl
      )
    )

    override def accepts(resource: String): Boolean = true
  }

  val url          = "file://some/url.json"
  val customUrl    = "file://some/other/url.json"
  val customLoader = new CustomContentUrlResourceLoader(customUrl)

  test("Location of Root matches location in SyamlParsedDocument when resource loader returns custom url") {

    val config = AMFGraphConfiguration.predefined().withResourceLoaders(List(customLoader))
    val context = new CompilerContextBuilder(url, platform, config.compilerConfiguration)
      .build()

    new AMFCompiler(context).root().map { root =>
      val document = root.parsed.asInstanceOf[SyamlParsedDocument]
      root.location shouldBe customUrl
      root.location shouldBe document.document.location.sourceName
    }
  }

  test("Test custom IdAdopter") {
    val parseplugin = new CustomParsePlugin(Document().withEncodes(ScalarNode("test", Some(XsdTypes.xsdString.iri()))))

    val urn = "urn:ms:an-org-id:api-contract:mulesoft:asset:g/a/v"
    val config = AMFGraphConfiguration
      .predefined()
      .withResourceLoaders(List(customLoader))
      .withRootParsePlugin(parseplugin)
      .withIdAdopterProvider(CustomTestIdAdopterProvider(urn))

    config.baseUnitClient().parse(url).map { result =>
      val document = result.baseUnit.asInstanceOf[Document]
      document.location().get shouldBe customUrl
      document.id shouldBe urn
    }
  }

  test("Test override filter of custom IdAdopter") {
    val moduleId = "amf://module-id"
    val module   = Module().withId(moduleId)
    val parseplugin = new CustomParsePlugin(
      Document()
        .withEncodes(ScalarNode("test", Some(XsdTypes.xsdString.iri())))
        .setArrayWithoutId(DocumentModel.References, Seq(module))
    )

    val urn = "urn:ms:an-org-id:api-contract:mulesoft:asset:g/a/v"
    val config = AMFGraphConfiguration
      .predefined()
      .withResourceLoaders(List(customLoader))
      .withRootParsePlugin(parseplugin)
      .withIdAdopterProvider(SkipReferencesIdAdopterProvider())

    config.baseUnitClient().parse(url).map { result =>
      val module = result.baseUnit.references.head
      module.id shouldBe moduleId
    }
  }

  class CustomParsePlugin(doc: Document) extends AMFParsePlugin {
    override def spec: Spec = Spec("Test")

    override def parse(document: Root, ctx: ParserContext): BaseUnit = doc.withLocation(document.location)

    /** media types which specifies vendors that are parsed by this plugin.
      */
    override def mediaTypes: Seq[String] = Seq("application/json")

    override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = SimpleReferenceHandler

    override def allowRecursiveReferences: Boolean = false

    override def applies(element: Root): Boolean = true

    override def priority: PluginPriority = HighPriority
  }

  case class CustomTestIdAdopterProvider(urn: String) extends IdAdopterProvider {
    override def idAdopter(initialId: String): IdAdopter = new IdAdopter(urn)

    override def idAdopter(initialId: String, idMaker: IdMaker): IdAdopter = new IdAdopter(urn, idMaker)
  }

  case class SkipReferencesIdAdopterProvider() extends IdAdopterProvider {

    case class CustomIdAdopter(initialId: String, idMaker: IdMaker = new DefaultIdMaker())
        extends IdAdopter(initialId, idMaker) {
      override def getOrderedFields(obj: AmfObject): Iterable[FieldEntry] = {
        obj match {
          case _: BaseUnit => BaseUnitFieldAdoptionOrdering.fields(obj).filterNot(_.field == DocumentModel.References)
          case _           => super.getOrderedFields(obj)
        }
      }
    }

    override def idAdopter(initialId: String): IdAdopter = CustomIdAdopter(initialId)

    override def idAdopter(initialId: String, idMaker: IdMaker): IdAdopter = CustomIdAdopter(initialId, idMaker)
  }

}
