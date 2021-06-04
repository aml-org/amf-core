package amf.core

import amf.client.plugins._
import amf.core.registries.AMFPluginsRegistry
import amf.core.validation.AMFPayloadValidationPlugin
import amf.plugins.document.graph.{AMFGraphPlugin}
import amf.plugins.syntax.SYamlSyntaxPlugin

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object AMF {

  private val initializedPlugins: mutable.Set[String] =
    mutable.Set(SYamlSyntaxPlugin.ID, AMFGraphPlugin.ID, SYamlSyntaxPlugin.ID)

  /**
    * Initializes AMF and all the registered plugins
    */
  def init()(implicit executionContext: ExecutionContext): Future[Unit] = {
    val registeredSYamlPlugin    = SYamlSyntaxPlugin.init()
    val registeredAMFGraphPlugin = AMFGraphPlugin.init()
    Future
      .sequence(Seq(registeredSYamlPlugin, registeredAMFGraphPlugin))
      .flatMap { _ =>
        processInitializations(AMFPluginsRegistry.plugins.toSeq)
      } map { _ =>
      AMFPluginsRegistry.registerSyntaxPlugin(SYamlSyntaxPlugin)
      AMFPluginsRegistry.registerDocumentPlugin(AMFGraphPlugin)
    }
  }

  /**
    * Registers a plugin in AMF
    */
  def registerPlugin(plugin: AMFPlugin): Unit = plugin match {
    case syntax: AMFSyntaxPlugin             => AMFPluginsRegistry.registerSyntaxPlugin(syntax)
    case document: AMFDocumentPlugin         => AMFPluginsRegistry.registerDocumentPlugin(document)
    case domain: AMFDomainPlugin             => AMFPluginsRegistry.registerDomainPlugin(domain)
    case feature: AMFFeaturePlugin           => AMFPluginsRegistry.registerFeaturePlugin(feature)
    case feature: AMFPayloadValidationPlugin => AMFPluginsRegistry.registerPayloadValidationPlugin(feature)
  }

  protected def processInitializations(plugins: Seq[AMFPlugin])(
      implicit executionContext: ExecutionContext): Future[Unit] = {
    if (plugins.isEmpty) {
      Future.successful(Unit)
    } else {
      val nextPlugin = plugins.head
      if (initializedPlugins.contains(nextPlugin.ID)) {

        processInitializations(plugins.tail)
      } else {
        val notInitializedYet = nextPlugin.dependencies().filter(plugin => !initializedPlugins.contains(plugin.ID))
        processInitializations(notInitializedYet) flatMap { _ =>
          nextPlugin.init() map { _ =>
            initializedPlugins += nextPlugin.ID
          }
        } flatMap { _ =>
          processInitializations(plugins.tail)
        }
      }
    }
  }
}
