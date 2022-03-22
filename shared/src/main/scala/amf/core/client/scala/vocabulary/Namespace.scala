package amf.core.client.scala.vocabulary

import amf.core.internal.annotations.Aliases

import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
  * Namespaces
  */
case class Namespace(base: String) {
  def +(id: String): ValueType = ValueType(this, id)
}

object Namespace {

  val Document: Namespace         = Namespace("http://a.ml/vocabularies/document#")
  val ApiContract: Namespace      = Namespace("http://a.ml/vocabularies/apiContract#")
  val Query: Namespace            = Namespace("http://a.ml/vocabularies/query#")
  val ApiBinding: Namespace       = Namespace("http://a.ml/vocabularies/apiBinding#")
  val Security: Namespace         = Namespace("http://a.ml/vocabularies/security#")
  val Shapes: Namespace           = Namespace("http://a.ml/vocabularies/shapes#")
  val Data: Namespace             = Namespace("http://a.ml/vocabularies/data#")
  val SourceMaps: Namespace       = Namespace("http://a.ml/vocabularies/document-source-maps#")
  val Shacl: Namespace            = Namespace("http://www.w3.org/ns/shacl#")
  val Core: Namespace             = Namespace("http://a.ml/vocabularies/core#")
  val Xsd: Namespace              = Namespace("http://www.w3.org/2001/XMLSchema#")
  val AnonShapes: Namespace       = Namespace("http://a.ml/vocabularies/shapes/anon#")
  val Rdf: Namespace              = Namespace("http://www.w3.org/1999/02/22-rdf-syntax-ns#")
  val WithoutNamespace: Namespace = Namespace("")
  val Meta: Namespace             = Namespace("http://a.ml/vocabularies/meta#")
  val Owl: Namespace              = Namespace("http://www.w3.org/2002/07/owl#")
  val Rdfs: Namespace             = Namespace("http://www.w3.org/2000/01/rdf-schema#")
  val AmfCore: Namespace          = Namespace("http://a.ml/vocabularies/amf/core#")
  val AmfAml: Namespace           = Namespace("http://a.ml/vocabularies/amf/aml#")
  val AmfParser: Namespace        = Namespace("http://a.ml/vocabularies/amf/parser#")
  val AmfResolution: Namespace    = Namespace("http://a.ml/vocabularies/amf/resolution#")
  val AmfValidation: Namespace    = Namespace("http://a.ml/vocabularies/amf/validation#")
  val AmfRender: Namespace        = Namespace("http://a.ml/vocabularies/amf/render#")

  def find(uri: String): Option[Namespace] = uri match {
    case "http://a.ml/vocabularies/document"             => Some(Document)
    case "http://a.ml/vocabularies/apiContract"          => Some(ApiContract)
    case "http://a.ml/vocabularies/apiBinding"           => Some(ApiBinding)
    case "http://a.ml/vocabularies/security"             => Some(Security)
    case "http://a.ml/vocabularies/shapes"               => Some(Shapes)
    case "http://a.ml/vocabularies/data"                 => Some(Data)
    case "http://a.ml/vocabularies/document-source-maps" => Some(SourceMaps)
    case "http://www.w3.org/ns/shacl"                    => Some(Shacl)
    case "http://a.ml/vocabularies/core#"                => Some(Core)
    case "http://www.w3.org/2001/XMLSchema"              => Some(Xsd)
    case "http://a.ml/vocabularies/shapes/anon"          => Some(AnonShapes)
    case "http://www.w3.org/1999/02/22-rdf-syntax-ns"    => Some(Rdf)
    case ""                                              => Some(WithoutNamespace)
    case "http://a.ml/vocabularies/meta"                 => Some(Meta)
    case "http://www.w3.org/2002/07/owl"                 => Some(Owl)
    case "http://www.w3.org/2000/01/rdf-schema"          => Some(Rdfs)
    case "http://a.ml/vocabularies/amf/parser"           => Some(AmfParser)
    case _                                               => None
  }

  object XsdTypes {
    val xsdString: ValueType  = Namespace.Xsd + "string"
    val xsdInteger: ValueType = Namespace.Xsd + "integer"
    val xsdFloat: ValueType   = Namespace.Xsd + "float"
    val amlNumber: ValueType  = Namespace.Shapes + "number"
    val amlLink: ValueType    = Namespace.Shapes + "link"
    val xsdDouble: ValueType  = Namespace.Xsd + "double"
    val xsdBoolean: ValueType = Namespace.Xsd + "boolean"
    val xsdNil: ValueType     = Namespace.Xsd + "nil"
    val xsdUri: ValueType     = Namespace.Xsd + "anyURI"
    val xsdAnyType: ValueType = Namespace.Xsd + "anyType"
    val amlAnyNode: ValueType = Namespace.Meta + "anyNode"
  }

  val defaultAliases: NamespaceAliases = NamespaceAliases()
}

case class NamespaceAliases private (ns: Map[Aliases.Alias, Namespace]) {

  def merge(aliases: NamespaceAliases): NamespaceAliases = NamespaceAliases(ns ++ aliases.ns)

  def uri(s: String): ValueType = {
    if (s.indexOf(":") > -1) {
      expand(s)
    } else {
      ns.values.find(n => s.indexOf(n.base) == 0) match {
        case Some(foundNs) => ValueType(foundNs, s.split(foundNs.base).last)
        case _             => ValueType(s)
      }
    }
  }

  def expand(uri: String): ValueType = {
    if (uri.startsWith("http://")) { // we have http: as  a valid prefix, we need to disambiguate
      ValueType(uri)
    } else {
      uri.split(":") match {
        case Array(prefix, postfix) =>
          resolve(prefix) match {
            case Some(n) => ValueType(n, postfix)
            case _       => ValueType(uri)
          }
        case _ => ValueType(uri)
      }
    }
  }

  def compact(uri: String): String = {
    ns.find {
      case (_, namespace) =>
        uri.indexOf(namespace.base) == 0
    } match {
      case Some((prefix, namespace)) =>
        prefix ++ uri.replace(namespace.base, ":")
      case None => uri
    }
  }

  def compactAndCollect(uri: String, prefixes: mutable.Map[String, String]): String = {
    ns.find {
      case (_, namespace) =>
        uri.indexOf(namespace.base) == 0
    } match {
      case Some((prefix, namespace)) =>
        prefixes.put(prefix, namespace.base)
        prefix ++ uri.replace(namespace.base, ":")
      case None => uri
    }
  }

  private def resolve(prefix: String): Option[Namespace] = ns.get(prefix)
}

object NamespaceAliases {

  def apply(): NamespaceAliases                                       = NamespaceAliases(knownAliases)
  def withCustomAliases(ns: Map[String, Namespace]): NamespaceAliases = NamespaceAliases(knownAliases ++ ns)

  // Leave as function, throws NullPointerException on initialization if it is val.
  private def knownAliases: Map[String, Namespace] = ListMap(
      "shacl"          -> Namespace.Shacl,
      "sh"             -> Namespace.Shacl,
      "raml-shapes"    -> Namespace.Shapes,
      "shapes"         -> Namespace.Shapes,
      "doc"            -> Namespace.Document,
      "raml-doc"       -> Namespace.Document,
      "rdf"            -> Namespace.Rdf,
      "security"       -> Namespace.Security,
      "core"           -> Namespace.Core,
      "xsd"            -> Namespace.Xsd,
      "amf-parser"     -> Namespace.AmfParser,
      "amf-core"       -> Namespace.AmfCore,
      "apiContract"    -> Namespace.ApiContract,
      "apiBinding"     -> Namespace.ApiBinding,
      "amf-resolution" -> Namespace.AmfResolution,
      "amf-validation" -> Namespace.AmfValidation,
      "amf-render"     -> Namespace.AmfRender,
      "data"           -> Namespace.Data,
      "sourcemaps"     -> Namespace.SourceMaps,
      "meta"           -> Namespace.Meta,
      "owl"            -> Namespace.Owl,
      "rdfs"           -> Namespace.Rdfs
  )
}
