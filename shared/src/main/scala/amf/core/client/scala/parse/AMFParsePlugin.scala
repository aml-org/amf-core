package amf.core.client.scala.parse

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler}
import amf.core.internal.parser.Root
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.remote.Spec

import scala.collection.GenTraversableOnce

trait AMFParsePlugin extends AMFPlugin[Root] {

  def spec: Spec

  override val id: String = spec.id

  def parse(document: Root, ctx: ParserContext): BaseUnit

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  def mediaTypes: Seq[String]

  def referencePlugins: Seq[AMFParsePlugin]

  def referenceHandler(eh: AMFErrorHandler): ReferenceHandler

  // move to some spec/dialect configuration?
  def allowRecursiveReferences: Boolean

  def withIdAdoption: Boolean = true
}
