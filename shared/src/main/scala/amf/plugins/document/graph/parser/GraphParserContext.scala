package amf.plugins.document.graph.parser

import amf.client.remod.ParseConfiguration
import amf.core.errorhandling.AMFErrorHandler
import amf.core.parser.{EmptyFutureDeclarations, FutureDeclarations, ParsedReference, ParserContext}
import amf.plugins.document.graph.context.GraphContext

class GraphParserContext(rootContextDocument: String = "",
                         refs: Seq[ParsedReference] = Seq.empty,
                         futureDeclarations: FutureDeclarations = EmptyFutureDeclarations(),
                         eh: AMFErrorHandler,
                         val graphContext: GraphContext = GraphContext())
    extends ParserContext(rootContextDocument, refs, futureDeclarations, ParseConfiguration(eh)) {}
