package amf.core.rdf

import amf.client.remod.{AMFGraphConfiguration, ParseConfiguration}
import amf.core.metamodel.document.BaseUnitModel
import amf.core.model.document._
import amf.core.model.domain._
import amf.core.rdf.graph.NodeFinder
import amf.core.rdf.parsers._
import amf.plugins.features.validation.CoreValidations.UnableToParseRdfDocument

object RdfModelParser {
  def apply(amfConfig: AMFGraphConfiguration): RdfModelParser =
    new RdfModelParser(ParseConfiguration(amfConfig, ""))
}

class RdfModelParser(parserConfig: ParseConfiguration) extends RdfParserCommon {

  override implicit val ctx: RdfParserContext = new RdfParserContext(eh = parserConfig.eh)
  def parse(model: RdfModel, location: String): BaseUnit = {
    val unit = model.findNode(location) match {
      case Some(rootNode) =>
        // assumes root is always an Obj
        val nodeFinder = new NodeFinder(model)
        val parser = new ObjectParser(location,
                                      new RecursionControl(),
                                      parserConfig.entitiesFacade,
                                      nodeFinder,
                                      new SourcesRetriever(nodeFinder))
        parser.parse(rootNode, findBaseUnit = true) match {
          case Some(unit: BaseUnit) =>
            unit.set(BaseUnitModel.Location, location.split("#").head)
            unit
          case _ =>
            ctx.eh.violation(UnableToParseRdfDocument,
                             location,
                             s"Unable to parse RDF model for location root node: $location")
            Document()
        }
      case _ =>
        ctx.eh.violation(UnableToParseRdfDocument,
                         location,
                         s"Unable to parse RDF model for location root node: $location")
        Document()
    }

    // Resolve annotations after parsing entire graph
    ctx.collected.collect({ case r: ResolvableAnnotation => r }) foreach (_.resolve(ctx.nodes))
    unit
  }
}

class RecursionControl(private var visited: Set[String] = Set()) {
  def visited(node: Node): Unit = {
    this.visited = visited + node.subject
  }
  def hasVisited(node: Node): Boolean               = visited.contains(node.subject)
  def hasVisited(property: PropertyObject): Boolean = visited.contains(property.value)
}
