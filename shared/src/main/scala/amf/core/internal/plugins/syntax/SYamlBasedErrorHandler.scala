package amf.core.internal.plugins.syntax

import amf.core.client.scala.parse.document.ErrorHandlingContext
import amf.core.internal.validation.CoreValidations.SyamlError
import amf.core.internal.validation.core.ValidationSpecification
import org.mulesoft.common.client.lexical.SourceLocation
import org.yaml.model.{
  IllegalTypeHandler,
  ParseErrorHandler,
  SyamlException,
  YDocument,
  YError,
  YFail,
  YNode,
  YPart,
  YSuccess
}

trait SYamlBasedErrorHandler extends IllegalTypeHandler with ParseErrorHandler { this: ErrorHandlingContext =>
  override def handle[T](error: YError, defaultValue: T): T = {
    eh.violation(SyamlError, "", error.error, part(error).location)
    defaultValue
  }

  final def handle(node: YPart, e: SyamlException): Unit = handle(node.location, e)

  override def handle(location: SourceLocation, e: SyamlException): Unit =
    eh.violation(SyamlError, "", e.getMessage, location)

  protected def part(error: YError): YPart = {
    error.node match {
      case d: YDocument => d
      case n: YNode     => n
      case s: YSuccess  => s.node
      case f: YFail     => part(f.error)
    }
  }

}
