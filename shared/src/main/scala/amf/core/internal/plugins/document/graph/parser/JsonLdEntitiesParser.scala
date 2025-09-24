package amf.core.internal.plugins.document.graph.parser

import amf.core.client.scala.model.domain.context.EntityModel
import amf.core.client.scala.vocabulary.ValueType
import amf.core.internal.parser._
import amf.core.internal.plugins.document.graph.context.GraphContext
import org.yaml.model._

case class JsonLdEntitiesParser(node: YNode, parserContext: GraphParserContext) {
  val context: GraphContext                     = parserContext.graphContext
  implicit val errorHandler: IllegalTypeHandler = parserContext

  def parse(): Unit = {
    node.tagType match {
      case YType.Seq => parseEntityList(node.value.asInstanceOf[YSequence])
      case _         => // Ignore
    }
  }

  private def parseEntityList(seq: YSequence)(implicit errorHandler: IllegalTypeHandler): Unit = {

    val entities = seq.nodes.flatMap { node =>
      node.tagType match {
        case YType.Map => parseEntity(node.value.asInstanceOf[YMap])
        case _         => None
      }
    }.toMap
    context.withContextEntities(entities)
  }

  private def parseEntity(map: YMap)(implicit errorHandler: IllegalTypeHandler): Option[(String, EntityModel)] = {
    map.entries.headOption.map { entity =>
      val entityIri = entity.key.as[YScalar].text
      val entityProps = entity.value.tagType match {
        case YType.Map => parseEntityProperties(entity.value.as[YMap])
        case _         => Nil
      }
      entityIri -> createEntityModel(entityIri, entityProps)
    }
  }

  private def parseEntityProperties(map: YMap)(implicit errorHandler: IllegalTypeHandler): List[(String, String)] =
    map.entries.map(parseEntityProperty).toList

  private def parseEntityProperty(entry: YMapEntry)(implicit errorHandler: IllegalTypeHandler): (String, String) = {
    val key   = entry.key.as[String]
    val value = entry.value.as[YScalar].text
    (key, value)
  }

  private def createEntityModel(entityIri: String, properties: List[(String, String)]): EntityModel = {
    EntityModel(ValueType(entityIri), properties.map(p => ValueType(p._1) -> ValueType(p._2)).toMap)
  }

}
