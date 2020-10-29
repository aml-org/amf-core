package amf.core.emitter
import org.scalatest.{Assertion, FunSuite, Matchers}
import org.yaml.builder.{BaseOutputBuilder, JsonOutputBuilder, YamlOutputBuilder}
import org.yaml.model.YNode
import org.yaml.parser.{JsonParser, YParser, YamlParser}

class YNodeDocBuilderPopulatorTest extends FunSuite with Matchers {

  case class CycleTest(name: String, source: String)

  val jsonCycles = Seq(
    CycleTest("root object with seq and scalar types",
      """
        |  {
        |    "key": [
        |      1,
        |      3.14,
        |      true,
        |      "string value"
        |    ]
        |  }
        |""".stripMargin),
    CycleTest("simple scalar type",
      """
        |  5
        |""".stripMargin),
    CycleTest("sequence at root",
      """
        |  [
        |    4,
        |    "string"
        |  ]
        |""".stripMargin)
  )

  val yamlCycles = Seq(
    CycleTest("root object with seq and scalar types",
      """
        |  int: 5
        |  string: some string
        |  float: 4.334
        |  boolean: false
        |  array:
        |    - other value""".stripMargin),
    CycleTest("simple scalar type",
      "5"),
    CycleTest("sequence at root",
      """
        |  - string value
        |  -
        |    other: obj""".stripMargin)
  )

  jsonCycles.foreach{ case CycleTest(name, source) =>
    test(s"json cycle - $name") { cycle(source, JsonParser(source), JsonOutputBuilder(true)) }
  }

  yamlCycles.foreach{ case CycleTest(name, source) =>
    test(s"yaml cycle - $name") { cycle(source, YamlParser(source), YamlOutputBuilder()) }
  }

  def cycle[T](source: String, parser: YParser, builder: BaseOutputBuilder[T]): Assertion = {
    val node: YNode = parser.documents().head.node
    YNodeDocBuilderPopulator.populate(node, builder)
    val result = builder.result.toString
    result shouldBe source
  }



}
