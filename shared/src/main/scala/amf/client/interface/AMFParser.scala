package amf.client.interface

import amf.client.convert.CoreClientConverters._

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.remod.{AMFParser => InternalAMFParser}

import scala.concurrent.ExecutionContext

@JSExportTopLevel("AMFParser")
@JSExportAll
object AMFParser {

  def parse(url: String, env: AMFGraphConfiguration): ClientFuture[AMFResult] = {
    implicit val context: ExecutionContext = env._internal.getExecutionContext
    InternalAMFParser.parse(url, env).asClient
  }

  def parse(url: String, mediaType: String, env: AMFGraphConfiguration): ClientFuture[AMFResult] = {
    implicit val context: ExecutionContext = env._internal.getExecutionContext
    InternalAMFParser.parse(url, mediaType, env).asClient
  }

  def parseContent(content: String, env: AMFGraphConfiguration): ClientFuture[AMFResult] = {
    implicit val context: ExecutionContext = env._internal.getExecutionContext
    InternalAMFParser.parseContent(content, env).asClient
  }

  def parseContent(content: String, mediaType: String, env: AMFGraphConfiguration): ClientFuture[AMFResult] = {
    implicit val context: ExecutionContext = env._internal.getExecutionContext
    InternalAMFParser.parseContent(content, mediaType, env).asClient
  }

  // TODO: content and url? no usage in mulesoft org so this can be ingored.
}
