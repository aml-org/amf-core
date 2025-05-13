package amf.core.internal.plugins.document.graph.emitter.utils

import amf.core.internal.annotations.{DeclaredElement, DefaultNode, TrackedElement, VirtualElement}

object SourceMapsAllowList {
  def apply(): List[String] = List(TrackedElement.name, DeclaredElement.name, VirtualElement.name, DefaultNode.name)
}
