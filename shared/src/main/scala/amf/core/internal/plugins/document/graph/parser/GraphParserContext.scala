package amf.core.internal.plugins.document.graph.parser

import amf.core.client.scala.parse.document.{EmptyFutureDeclarations, ParsedReference, ParserContext}
import amf.core.internal.parser.ParseConfiguration
import amf.core.internal.parser.domain.FutureDeclarations
import amf.core.internal.plugins.document.graph.context.GraphContext
import amf.core.internal.plugins.syntax.{SYamlAMFParserErrorHandler, SyamlAMFErrorHandler}
import org.mulesoft.lexer.SourceLocation
import org.yaml.model.{IllegalTypeHandler, ParseErrorHandler, SyamlException, YError}

class GraphParserContext(rootContextDocument: String = "",
                         refs: Seq[ParsedReference] = Seq.empty,
                         futureDeclarations: FutureDeclarations = EmptyFutureDeclarations(),
                         config: ParseConfiguration,
                         val graphContext: GraphContext = GraphContext())
    extends ParserContext(rootContextDocument, refs, futureDeclarations, config)
    with ParseErrorHandler
    with IllegalTypeHandler {

  override val eh = new SyamlAMFErrorHandler(config.eh)
  override def handle[T](error: YError, defaultValue: T): T = eh.handle(error, defaultValue)
  override def handle(location: SourceLocation, e: SyamlException): Unit = eh.handle(location, e)

}
