package amf.plugins.document.webapi.parser.spec.oas

import amf.core.Root
import amf.core.annotations.{ExplicitField, SingleValueArray, SourceVendor}
import amf.core.metamodel.Field
import amf.core.metamodel.document.{BaseUnitModel, ExtensionLikeModel}
import amf.core.metamodel.domain.DomainElementModel
import amf.core.metamodel.domain.extensions.CustomDomainPropertyModel
import amf.core.model.document.{BaseUnit, Document}
import amf.core.model.domain.extensions.CustomDomainProperty
import amf.core.model.domain.{AmfArray, AmfScalar}
import amf.core.parser.{Annotations, _}
import amf.core.utils.{Lazy, TemplateUri}
import amf.plugins.document.webapi.annotations._
import amf.plugins.document.webapi.contexts.OasWebApiContext
import amf.plugins.document.webapi.model.{Extension, Overlay}
import amf.plugins.document.webapi.parser.spec.common.{AnnotationParser, BaseSpecParser, SpecParserOps}
import amf.plugins.document.webapi.parser.spec.declaration.{AbstractDeclarationsParser, SecuritySchemeParser, _}
import amf.plugins.document.webapi.parser.spec.domain._
import amf.plugins.document.webapi.parser.spec.{OasDefinitions, _}
import amf.plugins.document.webapi.vocabulary.VocabularyMappings
import amf.plugins.domain.shapes.models.{CreativeWork, NodeShape}
import amf.plugins.domain.webapi.metamodel.EndPointModel._
import amf.plugins.domain.webapi.metamodel._
import amf.plugins.domain.webapi.metamodel.security.{OAuth2SettingsModel, ParametrizedSecuritySchemeModel, ScopeModel}
import amf.plugins.domain.webapi.models._
import amf.plugins.domain.webapi.models.security._
import amf.plugins.domain.webapi.models.templates.{ResourceType, Trait}
import org.yaml.model.{YNode, _}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Oas 2.0 spec parser
  */
case class OasDocumentParser(root: Root)(implicit val ctx: OasWebApiContext) extends OasSpecParser {

  def parseExtension(): Extension = {
    val extension = parseDocument(Extension())

    parseExtension(extension, ExtensionLikeModel.Extends)

    extension
  }

  private def parseExtension(document: Document, field: Field): Unit = {
    val map = root.parsed.document.as[YMap]
    UsageParser(map, document).parse()

    map
      .key("x-extends")
      .foreach(e => {
        ctx.link(e.value) match {
          case Left(url) =>
            root.references
              .find(_.origin.url == url)
              .foreach(extend =>
                document
                  .set(field, AmfScalar(extend.unit.id, Annotations(e.value)), Annotations(e)))
          case _ =>
        }
      })
  }

  def parseOverlay(): Overlay = {
    val overlay = parseDocument(Overlay())

    parseExtension(overlay, ExtensionLikeModel.Extends)

    overlay
  }

  def parseDocument(): Document = parseDocument(Document())

  private def parseDocument[T <: Document](document: T): T = {
    document.adopted(root.location).withLocation(root.location)

    val map = root.parsed.document.as[YMap]

    val references = ReferencesParser("x-uses", map, root.references).parse(root.location)
    parseDeclarations(root: Root, map)

    val api = parseWebApi(map).add(SourceVendor(root.vendor))
    document
      .withEncodes(api)
      .adopted(root.location)

    val declarable = ctx.declarations.declarables()
    if (declarable.nonEmpty) document.withDeclares(declarable)
    if (references.references.nonEmpty) document.withReferences(references.solvedReferences())

    document
  }

  def parseWebApi(map: YMap): WebApi = {

    val api = WebApi(map).adopted(root.location)

    map.key(
      "info",
      entry => {
        val info = entry.value.as[YMap]

        ctx.closedShape(api.id, info, "info")

        info.key("title", WebApiModel.Name in api)
        info.key("description", WebApiModel.Description in api)
        info.key("termsOfService", WebApiModel.TermsOfService in api)
        info.key("version", WebApiModel.Version in api)
        info.key("contact", WebApiModel.Provider in api using OrganizationParser.parse)
        info.key("license", WebApiModel.License in api using LicenseParser.parse)
      }
    )

    map.key("host", WebApiModel.Host in api)

    map.key(
      "x-base-uri-parameters",
      entry => {
        val uriParameters =
          OasHeaderParametersParser(entry.value.as[YMap], api.withBaseUriParameter).parse()
        api.set(WebApiModel.BaseUriParameters, AmfArray(uriParameters, Annotations(entry.value)), Annotations(entry))
      }
    )

    map.key("basePath", WebApiModel.BasePath in api)
    map.key("consumes", WebApiModel.Accepts in api)
    map.key("produces", WebApiModel.ContentType in api)
    map.key("schemes", WebApiModel.Schemes in api)

    map.key(
      "security",
      entry => {
        // TODO check for empty array for resolution ?
        val securedBy =
          entry.value
            .as[Seq[YNode]]
            .map(s => ParametrizedSecuritySchemeParser(s, api.withSecurity).parse())

        api.set(WebApiModel.Security, AmfArray(securedBy, Annotations(entry.value)), Annotations(entry))
      }
    )

    val documentations = ListBuffer[CreativeWork]()
    map.key(
      "externalDocs",
      entry => {
        documentations += OasCreativeWorkParser(entry.value.as[YMap]).parse()
      }
    )
    map.key(
      "x-user-documentation",
      entry => {
        documentations ++= UserDocumentationParser(entry.value.as[Seq[YNode]])
          .parse()
      }
    )

    if (documentations.nonEmpty)
      api.setArray(WebApiModel.Documentations, documentations)

    map.key(
      "paths",
      entry => {
        val paths = entry.value.as[YMap]
        paths.regex(
          "^/.*",
          entries => {
            val endpoints = mutable.ListBuffer[EndPoint]()
            entries.foreach(EndpointParser(_, api.withEndPoint, endpoints).parse())
            api.set(WebApiModel.EndPoints, AmfArray(endpoints), Annotations(entry.value))
          }
        )
      }
    )

    AnnotationParser(api, map).parse()

    ctx.closedShape(api.id, map, "webApi")

    api
  }

  case class ParametrizedSecuritySchemeParser(node: YNode, producer: String => ParametrizedSecurityScheme) {
    def parse(): ParametrizedSecurityScheme = node.to[YMap] match {
      case Right(map) =>
        val schemeEntry = map.entries.head
        val name        = schemeEntry.key
        val scheme      = producer(name).add(Annotations(map))

        var declaration = parseTarget(name, scheme, schemeEntry)
        declaration = declaration.linkTarget match {
          case Some(d) => d.asInstanceOf[SecurityScheme]
          case None    => declaration
        }

        if (declaration.`type` == "OAuth 2.0") {
          val settings = OAuth2Settings().adopted(scheme.id)
          val scopes = schemeEntry.value
            .as[Seq[YNode]]
            .map(n => Scope(n).set(ScopeModel.Name, AmfScalar(n.as[String]), Annotations(n)))

          scheme.set(ParametrizedSecuritySchemeModel.Settings,
                     settings.setArray(OAuth2SettingsModel.Scopes, scopes, Annotations(schemeEntry.value)))
        }

        scheme
      case _ =>
        val scheme = producer(node.toString)
        ctx.violation(scheme.id, s"Invalid type $node", node)
        scheme
    }

    private def parseTarget(name: String, scheme: ParametrizedSecurityScheme, part: YPart): SecurityScheme = {
      ctx.declarations.findSecurityScheme(name, SearchScope.All) match {
        case Some(declaration) =>
          scheme.set(ParametrizedSecuritySchemeModel.Scheme, declaration)
          declaration
        case None =>
          val securityScheme = SecurityScheme()
          scheme.set(ParametrizedSecuritySchemeModel.Scheme, securityScheme)
          ctx.violation(securityScheme.id, s"Security scheme '$name' not found in declarations.", part)
          securityScheme
      }
    }
  }

  case class EndpointParser(entry: YMapEntry, producer: String => EndPoint, collector: mutable.ListBuffer[EndPoint]) {

    def parse(): Unit = {
      val path = entry.key.as[String]

      val endpoint = producer(path).add(Annotations(entry))

      endpoint.set(Path, AmfScalar(path, Annotations(entry.key)))

      if (!TemplateUri.isValid(path))
        ctx.violation(endpoint.id, TemplateUri.invalidMsg(path), entry.value)

      if (collector.exists(e => e.path == path)) ctx.violation(endpoint.id, "Duplicated resource path " + path, entry)
      else parseEndpoint(endpoint)
    }

    private def parseEndpoint(endpoint: EndPoint) =
      entry.value.to[YMap] match {
        case Left(_) => collector += endpoint
        case Right(map) =>
          ctx.closedShape(endpoint.id, map, "pathItem")

          map.key("x-displayName", EndPointModel.Name in endpoint)
          map.key("x-description", EndPointModel.Description in endpoint)

          var parameters = OasParameters()
          val entries    = ListBuffer[YMapEntry]()
          map
            .key("parameters")
            .foreach(
              entry => {
                entries += entry
                parameters =
                  parameters.addFromOperation(ParametersParser(entry.value.as[Seq[YMap]], endpoint.id).parse())
                parameters.body.foreach(_.add(EndPointBodyParameter()))
              }
            )

          map
            .key("x-queryParameters")
            .foreach(
              entry => {
                entries += entry
                val queryParameters =
                  RamlParametersParser( // always 1.0 from oas, right?
                    entry.value.as[YMap],
                    (name: String) =>
                      Parameter().withName(name).adopted(endpoint.id))(toRaml(ctx)).parse().map(_.withBinding("query"))
                parameters = parameters.addFromOperation(OasParameters(query = queryParameters))
              }
            )

          map
            .key("x-headers")
            .foreach(
              entry => {
                entries += entry
                val headers =
                  RamlParametersParser(entry.value.as[YMap],
                                       (name: String) => Parameter().withName(name).adopted(endpoint.id))(toRaml(ctx))
                    .parse()
                    .map(_.withBinding("header"))
                parameters = parameters.addFromOperation(OasParameters(header = headers))
              }
            )

          parameters match {
            case OasParameters(_, path, _, _) if path.nonEmpty =>
              endpoint.set(EndPointModel.UriParameters, AmfArray(path, Annotations(entry.value)), Annotations(entry))
            case _ =>
          }

          map.key("x-is",
                  (EndPointModel.Extends in endpoint using ParametrizedDeclarationParser
                    .parse(endpoint.withTrait)).allowingSingleValue)

          map.key(
            "x-type",
            entry =>
              ParametrizedDeclarationParser(entry.value,
                                            endpoint.withResourceType,
                                            ctx.declarations.findResourceTypeOrError(entry.value))
                .parse()
          )

          collector += endpoint

          AnnotationParser(endpoint, map).parse()

          map.key(
            "x-security",
            entry => {
              // TODO check for empty array for resolution ?
              val securedBy = entry.value
                .as[Seq[YNode]]
                .map(s => ParametrizedSecuritySchemeParser(s, endpoint.withSecurity).parse())

              endpoint.set(OperationModel.Security, AmfArray(securedBy, Annotations(entry.value)), Annotations(entry))
            }
          )

          map.regex(
            "get|patch|put|post|delete|options|head",
            entries => {
              val operations = mutable.ListBuffer[Operation]()
              entries.foreach(entry => {
                operations += OperationParser(entry, parameters, endpoint.withOperation).parse()
              })
              endpoint.set(EndPointModel.Operations, AmfArray(operations))
            }
          )
      }
  }

  case class RequestParser(map: YMap, globalOrig: OasParameters, producer: () => Request)(
      implicit ctx: OasWebApiContext) {
    def parse(): Option[Request] = {
      val request = new Lazy[Request](producer)

      // we remove the path parameters to the empty becase the request
      // can overwrite the path parameters and this would be lost if were not
      // adding them here
      var parameters = globalOrig.copy(path = Seq())
      var entries    = ListBuffer[YMapEntry]()

      map
        .key("parameters")
        .foreach(
          entry => {
            entries += entry
            parameters = parameters.merge(ParametersParser(entry.value.as[Seq[YMap]], request.getOrCreate.id).parse())
          }
        )

      map
        .key("x-queryParameters")
        .foreach(
          entry => {
            entries += entry
            val queryParameters =
              RamlParametersParser(entry.value.as[YMap],
                                   (name: String) => Parameter().withName(name).adopted(request.getOrCreate.id))(
                toRaml(ctx)).parse().map(_.withBinding("query"))
            parameters = parameters.addFromOperation(OasParameters(query = queryParameters))
          }
        )

      map
        .key("x-headers")
        .foreach(
          entry => {
            entries += entry
            val headers =
              RamlParametersParser(entry.value.as[YMap],
                                   (name: String) => Parameter().withName(name).adopted(request.getOrCreate.id))(
                toRaml(ctx)).parse().map(_.withBinding("header"))
            parameters = parameters.addFromOperation(OasParameters(header = headers))
          }
        )

      // BaseUriParameters here are only valid for 0.8, must support the extention in RAml 1.0
      map.key(
        "x-baseUriParameters",
        entry => {
          entry.value.as[YMap].entries.headOption.foreach { paramEntry =>
            val parameter = Raml08ParameterParser(paramEntry, request.getOrCreate.withQueryParameter)(toRaml(ctx))
              .parse()
              .withBinding("path")
            parameters = parameters.addFromOperation(OasParameters(path = Seq(parameter)))
          }
        }
      )

      parameters match {
        case OasParameters(query, path, header, _) =>
          // query parameters and overwritten path parameters
          if (query.nonEmpty || path.nonEmpty)
            request.getOrCreate.set(RequestModel.QueryParameters,
                                    AmfArray(query ++ path, Annotations(entries.head)),
                                    Annotations(entries.head))
          if (header.nonEmpty)
            request.getOrCreate.set(RequestModel.Headers,
                                    AmfArray(header, Annotations(entries.head)),
                                    Annotations(entries.head))

          if (path.nonEmpty)
            request.getOrCreate.set(RequestModel.BaseUriParameters,
                                    AmfArray(path, Annotations(entries.head)),
                                    Annotations(entries.head))
      }

      val payloads = mutable.ListBuffer[Payload]()
      parameters.body.foreach(payloads += _)

      map.key(
        "x-request-payloads",
        entry =>
          entry.value
            .as[Seq[YMap]]
            .map(value => payloads += OasPayloadParser(value, request.getOrCreate.withPayload).parse())
      )

      if (payloads.nonEmpty) request.getOrCreate.set(RequestModel.Payloads, AmfArray(payloads))

      map.key(
        "x-queryString",
        queryEntry => {
          Raml10TypeParser(queryEntry, (shape) => shape.adopted(request.getOrCreate.id))
            .parse()
            .map(request.getOrCreate.withQueryString(_))
        }
      )

      request.option
    }
  }

  case class OperationParser(entry: YMapEntry, global: OasParameters, producer: String => Operation) {
    def parse(): Operation = {

      val operation = producer(ScalarNode(entry.key).string().value.toString).add(Annotations(entry))
      val map       = entry.value.as[YMap]

      map.key("operationId", OperationModel.Name in operation)
      map.key("description", OperationModel.Description in operation)
      map.key("deprecated", OperationModel.Deprecated in operation)
      map.key("summary", OperationModel.Summary in operation)
      map.key("externalDocs", OperationModel.Documentation in operation using OasCreativeWorkParser.parse)
      map.key("schemes", OperationModel.Schemes in operation)
      map.key("consumes", OperationModel.Accepts in operation)
      map.key("produces", OperationModel.ContentType in operation)

      map.key(
        "x-is",
        entry => {
          val traits = entry.value
            .as[Seq[YNode]]
            .map(value => {
              ParametrizedDeclarationParser(value, operation.withTrait, ctx.declarations.findTraitOrError(value))
                .parse()
            })
          if (traits.nonEmpty) operation.setArray(DomainElementModel.Extends, traits, Annotations(entry))
        }
      )

      map.key(
        "security",
        entry => {
          // TODO check for empty array for resolution ?
          val securedBy = entry.value
            .as[Seq[YNode]]
            .map(s => ParametrizedSecuritySchemeParser(s, operation.withSecurity).parse())

          operation.set(OperationModel.Security, AmfArray(securedBy, Annotations(entry.value)), Annotations(entry))
        }
      )

      RequestParser(map, global, () => operation.withRequest())
        .parse()
        .map(operation.set(OperationModel.Request, _))

      map.key(
        "responses",
        entry => {
          entry.value
            .as[YMap]
            .regex(
              "default|\\d{3}",
              entries => {
                val responses = mutable.ListBuffer[Response]()
                entries.foreach(entry => {
                  responses += OasResponseParser(entry, operation.withResponse).parse()
                })
                operation.set(OperationModel.Responses,
                              AmfArray(responses, Annotations(entry.value)),
                              Annotations(entry))
              }
            )
        }
      )

      AnnotationParser(operation, map).parse()

      ctx.closedShape(operation.id, map, "operation")

      operation
    }
  }
}

abstract class OasSpecParser(implicit ctx: OasWebApiContext) extends BaseSpecParser with SpecParserOps {

  protected def parseDeclarations(root: Root, map: YMap): Unit = {
    val parent = root.location + "#/declarations"
    parseTypeDeclarations(map, parent)
    parseAnnotationTypeDeclarations(map, parent)
    AbstractDeclarationsParser("x-resourceTypes", (entry: YMapEntry) => ResourceType(entry), map, parent)
      .parse()
    AbstractDeclarationsParser("x-traits", (entry: YMapEntry) => Trait(entry), map, parent).parse()
    parseSecuritySchemeDeclarations(map, parent)
    parseParameterDeclarations(map, parent)
    parseResponsesDeclarations("responses", map, parent)
    ctx.futureDeclarations.resolve()

  }

  def parseAnnotationTypeDeclarations(map: YMap, customProperties: String): Unit = {

    map.key(
      "x-annotationTypes",
      e => {
        e.value
          .as[YMap]
          .entries
          .map(entry => {
            val typeName = entry.key.as[YScalar].text
            val customProperty = AnnotationTypesParser(entry,
                                                       customProperty =>
                                                         customProperty
                                                           .withName(typeName)
                                                           .adopted(customProperties))
            ctx.declarations += customProperty.add(DeclaredElement())
          })
      }
    )
  }

  def parseTypeDeclarations(map: YMap, typesPrefix: String): Unit = {

    map.key(
      "definitions",
      entry => {
        entry.value
          .as[YMap]
          .entries
          .foreach(e => {
            val typeName = e.key.as[YScalar].text
            OasTypeParser(e, shape => shape.withName(typeName).adopted(typesPrefix))
              .parse() match {
              case Some(shape) =>
                ctx.declarations += shape.add(DeclaredElement())
              case None =>
                ctx.violation(NodeShape().adopted(typesPrefix).id, s"Error parsing shape at $typeName", e)
            }
          })
      }
    )
  }

  private def parseSecuritySchemeDeclarations(map: YMap, parent: String): Unit = {
    map.key(
      "securityDefinitions",
      e => {
        e.value.as[YMap].entries.foreach { entry =>
          ctx.declarations += SecuritySchemeParser(entry, scheme => scheme.withName(entry.key).adopted(parent))
            .parse()
            .add(DeclaredElement())
        }
      }
    )

    map.key(
      "x-securitySchemes",
      e => {
        e.value.as[YMap].entries.foreach { entry =>
          ctx.declarations += SecuritySchemeParser(entry, scheme => scheme.withName(entry.key).adopted(parent))
            .parse()
            .add(DeclaredElement())
        }
      }
    )
  }

  def parseParameterDeclarations(map: YMap, parentPath: String): Unit = {
    map.key(
      "parameters",
      entry => {
        entry.value
          .as[YMap]
          .entries
          .foreach(e => {
            val typeName = e.key.as[YScalar].text
            val oasParameter = e.value.to[YMap] match {
              case Right(m) => ParameterParser(m, parentPath).parse()
              case _ =>
                val parameter = ParameterParser(YMap.empty, parentPath).parse()
                ctx.violation(parameter.parameter.id, "Map needed to parse a parameter declaration", e)
                parameter
            }

            val parameter = oasParameter.parameter.withName(typeName).add(DeclaredElement())
            parameter.fields.getValue(ParameterModel.Binding).annotations += ExplicitField()
            ctx.declarations.registerParameter(parameter, oasParameter.payload)
          })
      }
    )
  }

  def parseResponsesDeclarations(key: String, map: YMap, parentPath: String): Unit = {
    map.key(
      key,
      entry => {
        entry.value
          .as[YMap]
          .entries
          .foreach(e => {
            ctx.declarations +=
              OasResponseParser(e, (name: String) => Response().withName(name).adopted(parentPath))
                .parse()
                .add(DeclaredElement())

          })
      }
    )
  }

  case class UsageParser(map: YMap, baseUnit: BaseUnit) {
    def parse(): Unit = {
      map.key("x-usage", entry => {
        val value = ScalarNode(entry.value)
        baseUnit.set(BaseUnitModel.Usage, value.string(), Annotations(entry))
      })
    }
  }

  object AnnotationTypesParser {
    def apply(ast: YMapEntry, adopt: (CustomDomainProperty) => Unit): CustomDomainProperty =
      ast.value.tagType match {
        case YType.Map =>
          ast.value.as[YMap].key("$ref") match {
            case Some(reference) => {
              LinkedAnnotationTypeParser(ast, reference.value.as[YScalar].text, reference.value.as[YScalar], adopt)
                .parse()
            }
            case _ => AnnotationTypesParser(ast, ast.key.as[YScalar].text, ast.value.as[YMap], adopt).parse()
          }
        case YType.Seq =>
          val customDomainProperty = CustomDomainProperty().withName(ast.key.as[YScalar].text)
          adopt(customDomainProperty)
          ctx.violation(
            customDomainProperty.id,
            "Invalid value node type for annotation types parser, expected map or scalar reference",
            ast.value
          )
          customDomainProperty
        case _ =>
          LinkedAnnotationTypeParser(ast, ast.key.as[YScalar].text, ast.value.as[YScalar], adopt).parse()
      }

  }

  case class LinkedAnnotationTypeParser(ast: YPart,
                                        annotationName: String,
                                        scalar: YScalar,
                                        adopt: (CustomDomainProperty) => Unit) {
    def parse(): CustomDomainProperty = {
      ctx.declarations
        .findAnnotation(scalar.text, SearchScope.All)
        .map { a =>
          val copied: CustomDomainProperty = a.link(scalar.text, Annotations(ast))
          copied.id = null // we reset the ID so ti can be adopted, there's an extra rule where the id is not set
          // because the way they are inserted in the mode later in the parsing
          adopt(copied.withName(annotationName))
          copied
        }
        .getOrElse {
          val customDomainProperty = CustomDomainProperty().withName(annotationName)
          adopt(customDomainProperty)
          ctx.violation(customDomainProperty.id, "Could not find declared annotation link in references", scalar)
          customDomainProperty
        }
    }
  }

  case class AnnotationTypesParser(ast: YPart,
                                   annotationName: String,
                                   map: YMap,
                                   adopt: (CustomDomainProperty) => Unit) {
    def parse(): CustomDomainProperty = {
      val custom = CustomDomainProperty(ast)
      custom.withName(annotationName)
      adopt(custom)

      map.key(
        "allowedTargets",
        entry => {
          val annotations = Annotations(entry)
          val targets: AmfArray = entry.value.value match {
            case _: YScalar =>
              annotations += SingleValueArray()
              AmfArray(Seq(ScalarNode(entry.value).text()))
            case sequence: YSequence =>
              ArrayNode(sequence).text()
          }

          val targetUris = targets.values.map({
            case s: AmfScalar =>
              VocabularyMappings.ramlToUri.get(s.toString) match {
                case Some(uri) => AmfScalar(uri, s.annotations)
                case None      => s
              }
            case nodeType => AmfScalar(nodeType.toString, nodeType.annotations)
          })

          custom.set(CustomDomainPropertyModel.Domain, AmfArray(targetUris), annotations)
        }
      )

      map.key("displayName", entry => {
        val value = ScalarNode(entry.value)
        custom.set(CustomDomainPropertyModel.DisplayName, value.string(), Annotations(entry))
      })

      map.key("description", entry => {
        val value = ScalarNode(entry.value)
        custom.set(CustomDomainPropertyModel.Description, value.string(), Annotations(entry))
      })

      map.key(
        "schema",
        entry => {
          OasTypeParser(entry, shape => shape.adopted(custom.id))
            .parse()
            .foreach({ shape =>
              custom.set(CustomDomainPropertyModel.Schema, shape, Annotations(entry))
            })
        }
      )

      AnnotationParser(custom, map).parse()

      custom
    }
  }

  case class UserDocumentationParser(seq: Seq[YNode]) {
    def parse(): Seq[CreativeWork] =
      seq.map(n =>
        n.tagType match {
          case YType.Map => RamlCreativeWorkParser(n.as[YMap]).parse()
          case YType.Str =>
            val text = n.as[YScalar].text
            ctx.declarations.findDocumentations(text, SearchScope.All) match {
              case Some(doc) => doc.link(text, Annotations(n)).asInstanceOf[CreativeWork]
              case _ =>
                val documentation = RamlCreativeWorkParser(YMap.empty).parse()
                ctx.violation(documentation.id, s"not supported scalar $n.text for documentation item", n)
                documentation
            }
      })
  }

  case class ParameterParser(map: YMap, parentId: String) {
    def parse(): OasParameter = {
      map.key("$ref") match {
        case Some(ref) => parseParameterRef(ref, parentId)
        case None =>
          val p         = OasParameter(map)
          val parameter = p.parameter

          parameter.set(ParameterModel.Required, value = false)

          map.key("name", ParameterModel.Name in parameter)
          map.key("description", ParameterModel.Description in parameter)
          map.key("required", (ParameterModel.Required in parameter).explicit)
          map.key("in", ParameterModel.Binding in parameter)

          // TODO generate parameter with parent id or adopt
          if (p.isBody) {
            p.payload.adopted(parentId)
            map.key(
              "schema",
              entry => {
                OasTypeParser(entry, (shape) => shape.withName("schema").adopted(p.payload.id))
                  .parse()
                  .map(p.payload.set(PayloadModel.Schema, _, Annotations(entry)))
              }
            )

            map.key("x-media-type", PayloadModel.MediaType in p.payload)

          } else {
            // type
            parameter.adopted(parentId)

            ctx.closedShape(parameter.id, map, "parameter")

            OasTypeParser(
              map,
              "",
              map,
              shape => shape.withName("schema").adopted(parameter.id),
              "parameter"
            ).parse()
              .map(parameter.set(ParameterModel.Schema, _, Annotations(map)))
          }

          AnnotationParser(parameter, map).parse()

          p
      }
    }

    protected def parseParameterRef(ref: YMapEntry, parentId: String): OasParameter = {
      val refUrl = OasDefinitions.stripParameterDefinitionsPrefix(ref.value)
      ctx.declarations.findParameter(refUrl, SearchScope.All) match {
        case Some(p) =>
          val payload: Payload     = ctx.declarations.parameterPayload(p)
          val parameter: Parameter = p.link(refUrl, Annotations(map))
          parameter.withName(refUrl).adopted(parentId)
          OasParameter(parameter, payload)
        case None =>
          val oasParameter = OasParameter(Parameter(YMap.empty), Payload(YMap.empty))
          ctx.violation(oasParameter.parameter.id, s"Cannot find parameter reference $refUrl", ref)
          oasParameter
      }
    }
  }

  case class ParametersParser(values: Seq[YMap], parentId: String) {
    def parse(): OasParameters = {
      val parameters = values
        .map(value => ParameterParser(value, parentId).parse())

      OasParameters(
        parameters.filter(_.isQuery).map(_.parameter),
        parameters.filter(_.isPath).map(_.parameter),
        parameters.filter(_.isHeader).map(_.parameter),
        parameters.filter(_.isBody).map(_.payload).headOption
      )
    }
  }

  case class OasParameters(query: Seq[Parameter] = Nil,
                           path: Seq[Parameter] = Nil,
                           header: Seq[Parameter] = Nil,
                           body: Option[Payload] = None) {
    def merge(inner: OasParameters): OasParameters = {
      OasParameters(merge(query, inner.query),
                    merge(path, inner.path),
                    merge(header, inner.header),
                    merge(body, inner.body))
    }

    def addFromOperation(inner: OasParameters): OasParameters = {
      OasParameters(add(query, inner.query), add(path, inner.path), add(header, inner.header), add(body, inner.body))
    }

    private def merge(global: Option[Payload], inner: Option[Payload]): Option[Payload] =
      inner.map(_.add(DefaultPayload())).orElse(global.map(_.copy()))

    private def add(global: Option[Payload], inner: Option[Payload]): Option[Payload] =
      inner.map(_.add(DefaultPayload())).orElse(global.map(_.copy()))

    private def merge(global: Seq[Parameter], inner: Seq[Parameter]): Seq[Parameter] = {
      val globalMap = global.map(p => p.name -> p.copy().add(EndPointParameter())).toMap
      val innerMap  = inner.map(p => p.name  -> p.copy()).toMap

      (globalMap ++ innerMap).values.toSeq
    }

    private def add(global: Seq[Parameter], inner: Seq[Parameter]): Seq[Parameter] = {
      val globalMap = global.map(p => p.name -> p).toMap
      val innerMap  = inner.map(p => p.name  -> p).toMap

      (globalMap ++ innerMap).values.toSeq
    }
  }

  case class OasParameter(parameter: Parameter, payload: Payload) {
    def isBody: Boolean   = parameter.isBody
    def isQuery: Boolean  = parameter.isQuery
    def isPath: Boolean   = parameter.isPath
    def isHeader: Boolean = parameter.isHeader
  }

  object OasParameter {
    def apply(ast: YMap): OasParameter = OasParameter(Parameter(ast), Payload(ast))
  }

}

case class OasParameter(parameter: Parameter, payload: Payload) {
  def isBody: Boolean   = parameter.isBody
  def isQuery: Boolean  = parameter.isQuery
  def isPath: Boolean   = parameter.isPath
  def isHeader: Boolean = parameter.isHeader
}

object OasParameter {
  def apply(ast: YMap): OasParameter = OasParameter(Parameter(ast), Payload(ast))
}
