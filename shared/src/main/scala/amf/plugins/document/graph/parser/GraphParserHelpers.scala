package amf.plugins.document.graph.parser

import amf.core.metamodel.Type
import amf.core.metamodel.Type._
import amf.core.metamodel.document.SourceMapModel
import amf.core.metamodel.domain.DomainElementModel
import amf.core.model.document.SourceMap
import amf.core.model.domain.{AmfElement, Annotation}
import amf.core.parser.{Annotations, _}
import amf.core.vocabulary.Namespace
import amf.core.vocabulary.Namespace.SourceMaps
import amf.plugins.features.validation.CoreValidations.{MissingIdInNode, MissingTypeInNode}
import org.yaml.convert.YRead.SeqNodeYRead
import org.yaml.model._

import scala.collection.mutable

trait GraphParserHelpers extends GraphContextHelper {

  private def parseSourceNode(map: YMap)(implicit ctx: GraphParserContext): SourceMap = {
    val result = SourceMap()
    map.entries.foreach(entry => {
      entry.key.toOption[YScalar].map(value => expandUriFromContext(value.text)).foreach {
        case AnnotationName(annotation) =>
          val consumer = result.annotation(annotation)
          entry.value
            .as[Seq[YNode]]
            .foreach(e => {
              contentOfNode(e) foreach { element =>
                val k       = element.key(compactUriFromContext(SourceMapModel.Element.value.iri())).get
                val v       = element.key(compactUriFromContext(SourceMapModel.Value.value.iri())).get
                consumer(value(SourceMapModel.Element.`type`, k.value).as[YScalar].text,
                  value(SourceMapModel.Value.`type`, v.value).as[YScalar].text)
              }
            })
        case _ => // Unknown annotation identifier
      }
    })
    result
  }

  protected def ts(map: YMap, id: String)(implicit ctx: GraphParserContext): Seq[String] = {
    val namespaces =
      Seq("Document", "Fragment", "Module", "Unit").map(docElement => (Namespace.Document + docElement).iri())

    val documentTypesSet: Set[String] = (namespaces ++ namespaces.map(compactUriFromContext(_))).toSet

    map.key("@type") match {
      case Some(entry) =>
        val allTypes         = entry.value.toOption[Seq[YNode]].getOrElse(Nil).flatMap(v => v.toOption[YScalar].map(_.text))
        val nonDocumentTypes = allTypes.filter(t => !documentTypesSet.contains(t))
        val documentTypes    = allTypes.filter(t => documentTypesSet.contains(t)).sorted // we just use the fact that lexical order is correct
        nonDocumentTypes ++ documentTypes

      case _ =>
        ctx.eh.violation(MissingTypeInNode, id, s"No @type declaration on node $map", map) // todo : review with pedro
        Nil
    }
  }

  protected def retrieveId(map: YMap, ctx: ParserContext): Option[String] = {
    map.key("@id") match {
      case Some(entry) => Some(entry.value.as[YScalar].text)
      case _ =>
        ctx.eh.violation(MissingIdInNode, "", s"No @id declaration on node $map", map)
        None
    }
  }

  protected def contentOfNode(n: YNode): Option[YMap] = n.toOption[YMap]

  protected def retrieveSources(id: String, map: YMap)(implicit ctx: GraphParserContext): SourceMap = {
    map
      .key(compactUriFromContext(DomainElementModel.Sources.value.iri()))
      .flatMap { entry =>
        val srcNode = value(SourceMapModel, entry.value)
        contentOfNode(srcNode).map(parseSourceNode(_))
      }
      .getOrElse(SourceMap.empty)
  }

  protected def value(t: Type, node: YNode): YNode = {
    node.tagType match {
      case YType.Seq =>
        t match {
          case Array(_) => node
          case _        => value(t, node.as[Seq[YNode]].head)
        }
      case YType.Map =>
        val m: YMap = node.as[YMap]
        t match {
          case Iri                                       => m.key("@id").get.value
          case Str | RegExp | Bool | Type.Int | Type.Any => m.key("@value").get.value
          case _                                         => node
        }
      case _ => node
    }
  }

  protected object AnnotationName {
    def unapply(uri: String): Option[String] = uri match {
      case url if url.startsWith(SourceMaps.base) => Some(url.substring(url.indexOf("#") + 1))
      case _                                      => None
    }
  }

  protected def annotations(nodes: Map[String, AmfElement], sources: SourceMap, key: String): Annotations = {
    val result = Annotations()

    if (sources.nonEmpty) {
      sources.annotations.foreach {
        case (annotation, values: mutable.Map[String, String]) =>
          annotation match {
            case Annotation(deserialize) if values.contains(key) =>
              deserialize(values(key), nodes).foreach(result += _)
            case _ =>
          }
      }
    }

    result
  }
}

abstract class GraphContextHelper {

  protected def expandUriFromContext(iri: String)(implicit ctx: GraphParserContext): String = {
    ctx.compactUris.find { case (key, _) => iri.startsWith(key) } match {
      case Some((key, value)) => iri.replace(key + ':', value)
      case None               => iri
    }
  }

  protected def compactUriFromContext(iri: String)(implicit ctx: GraphParserContext): String = {
    ctx.compactUris.find { case (_, value) => iri.startsWith(value) } match {
      case Some((key, value)) => iri.replace(value, key + ':')
      case None               => iri
    }
  }

  private def baseParent(base: String): String = {
    val idx = base.lastIndexOf("/")
    base.substring(0, idx)
  }

  protected def transformIdFromContext(id: String)(implicit ctx: GraphParserContext): String = {
    val prefixOption = ctx.baseId.map { base =>
      if (id.startsWith("./")) baseParent(base) + "/"
      else base
    }
    val prefix = prefixOption.getOrElse("")

    s"$prefix$id"
  }

  protected def parseCompactUris(contextNode: YNode)(implicit ctx: GraphParserContext): Unit = {
    ctx.compactUris ++= buildContextMap(contextNode)
    ctx.baseId = ctx.compactUris.find { case (key, _) => key == "@base" }.map { case (_, value) => value }
  }

  protected def parseKeyValue(entry: YMapEntry): Option[(String, String)] = {
    (entry.key.tagType, entry.value.tagType) match {
      case (YType.Str, YType.Str) =>
        Some(entry.key.as[YScalar].text -> entry.value.as[YScalar].text)
      case _ => None
    }
  }

  protected def buildContextMap(contextNode: YNode): Map[String, String] = {
    contextNode.tagType match {
      case YType.Map =>
        val m: YMap = contextNode.as[YMap]
        m.entries.flatMap(parseKeyValue).toMap
      case _ => Map.empty
    }
  }

}
