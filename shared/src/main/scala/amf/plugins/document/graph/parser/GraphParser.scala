package amf.plugins.document.graph.parser
import amf.client.remod.ParseConfiguration
import amf.core.model.document.{BaseUnit, SourceMap}
import amf.core.model.domain.AmfElement
import amf.core.parser.{Annotations, SyamlParsedDocument}
import org.yaml.model.YDocument

abstract class GraphParser(parserConfig: ParseConfiguration) extends GraphParserHelpers {
  def canParse(document: SyamlParsedDocument): Boolean
  def parse(document: YDocument, location: String): BaseUnit

  def annotations(nodes: Map[String, AmfElement], sources: SourceMap, key: String): Annotations =
    parserConfig.serializableAnnotationsFacade.retrieveAnnotation(nodes, sources, key)
}
