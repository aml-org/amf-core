package amf.apicontract.internal.spec.raml.parser.external.json

import amf.apicontract.internal.spec.common.parser.WebApiShapeParserContextAdapter
import amf.apicontract.internal.spec.oas.parser.context.OasWebApiContext
import amf.apicontract.internal.spec.raml.parser.context.RamlWebApiContext
import amf.apicontract.internal.spec.raml.parser.external.RamlJsonSchemaParser.{errorShape, withScopedContext}
import amf.core.internal.parser.domain.JsonParserFactory
import amf.core.internal.utils.UriUtils
import amf.shapes.client.scala.model.domain.{AnyShape, UnresolvedShape}
import amf.shapes.internal.annotations.ParsedJSONSchema
import amf.shapes.internal.spec.ShapeParserContext
import amf.shapes.internal.spec.common.parser.ExternalFragmentHelper.searchForAlreadyParsedNodeInFragments
import amf.shapes.internal.spec.jsonschema.parser.JsonSchemaParsingHelper
import amf.shapes.internal.spec.oas.parser.OasTypeParser
import amf.shapes.internal.spec.raml.parser.external.ValueAndOrigin
import org.mulesoft.lexer.Position
import org.yaml.model.{YMapEntry, YNode}
import org.yaml.parser.JsonParser

class LegacyRootJsonSchemaParser(key: YNode, ast: YNode)(implicit ctx: RamlWebApiContext) {

  private val shapeCtx: ShapeParserContext = WebApiShapeParserContextAdapter(ctx)

  def parse(origin: ValueAndOrigin, basePath: String) = {
    val shape = parseJsonShape(origin.text, key, origin.valueAST, ast, origin.originalUrlText)
    ctx.declarations.fragments
      .get(basePath)
      .foreach(e => shape.callAfterAdoption(() => shape.withReference(e.encoded.id)))
    ctx.declarations.registerExternalRef(basePath, shape)
    shape.annotations += ParsedJSONSchema(origin.text.trim)
    shape
  }

  private def parseJsonShape(
      text: String,
      key: YNode,
      valueAST: YNode,
      value: YNode,
      extLocation: Option[String]
  ): AnyShape = {

    val node: YNode = parseAst(text, valueAST, extLocation)
    val schemaEntry = YMapEntry(key, node)
    val shape = withScopedContext(valueAST, schemaEntry) { jsonSchemaContext =>
      val jsonSchemaShapeContext = WebApiShapeParserContextAdapter(jsonSchemaContext)

      val fullRef = UriUtils.normalizePath(jsonSchemaContext.rootContextDocument)

      val tmpShape: UnresolvedShape = initializedUnresolved(schemaEntry, jsonSchemaShapeContext, fullRef)

      val parsed = parse(value, schemaEntry, jsonSchemaShapeContext, fullRef, tmpShape)
      propagatePromotedFragments(jsonSchemaContext)
      parsed
    }
    shape
  }

  private def initializedUnresolved(
      schemaEntry: YMapEntry,
      jsonSchemaShapeContext: WebApiShapeParserContextAdapter,
      fullRef: String
  ) = {
    JsonSchemaParsingHelper.createTemporaryShape(
      _ => {},
      schemaEntry,
      jsonSchemaShapeContext,
      fullRef
    )
  }

  private def parseAst(text: String, valueAST: YNode, extLocation: Option[String]) = {
    searchForAlreadyParsedNodeInFragments(valueAST)(shapeCtx).getOrElse {
      jsonParser(text, valueAST, extLocation).document().node
    }
  }

  private def jsonParser(text: String, valueAST: YNode): JsonParser = {
    JsonParserFactory.fromCharsWithSource(
      text,
      valueAST.value.sourceName,
      Position(valueAST.range.lineFrom, valueAST.range.columnFrom)
    )(ctx.eh)
  }
  private def jsonParser(text: String, valueAST: YNode, extLocation: Option[String]): JsonParser = {
    val url = extLocation.flatMap(ctx.declarations.fragments.get).flatMap(_.location)
    url
      .map { JsonParserFactory.fromCharsWithSource(text, _)(ctx.eh) }
      .getOrElse(jsonParser(text, valueAST))
  }

  private def propagatePromotedFragments(jsonSchemaContext: OasWebApiContext): Unit = {
    if (jsonSchemaContext.declarations.promotedFragments.nonEmpty) {
      ctx.declarations.promotedFragments ++= jsonSchemaContext.declarations.promotedFragments
    }
  }

  private def parse(
      value: YNode,
      schemaEntry: YMapEntry,
      jsonSchemaContext: ShapeParserContext,
      fullRef: String,
      tmpShape: UnresolvedShape
  ) = {
    OasTypeParser(schemaEntry, _ => {}, ctx.computeJsonSchemaVersion(schemaEntry.value))(jsonSchemaContext)
      .parse() match {
      case Some(sh) =>
        ctx.futureDeclarations.resolveRef(fullRef, sh)
        ctx.registerJsonSchema(fullRef, sh)
        tmpShape.resolve(sh) // useless?
        if (sh.isLink) sh.effectiveLinkTarget().asInstanceOf[AnyShape]
        else sh
      case None => errorShape(value)
    }
  }
}
