package amf.core.internal.utils

import scala.annotation.tailrec
import scala.collection.mutable

object GraphCycleDetector {
  type Graph[N] = mutable.Map[N, Set[N]]

  private case class NodeWithBranch[N](node: N, branch: List[N])

  def hasCycles[N](graph: Graph[N], initialNode: N): Boolean = {
    // Mutable collections are used to improve performance
    val allVisited = mutable.Set[N]()

    @tailrec
    def innerHasCycles(pendingNodes: List[NodeWithBranch[N]]): Boolean = pendingNodes match {
      case Nil => false
      case ::(NodeWithBranch(currentNode, currentBranch), remainingNodes) =>
        if (currentBranch.contains(currentNode)) {
          // There is a cycle
          return true
        }

        if (allVisited.contains(currentNode)) {
          // This node has already been visited
          innerHasCycles(remainingNodes)
        } else {
          allVisited.add(currentNode)
          val nextBranch: List[N] = currentNode +: currentBranch

          val newNodes             = graph.getOrElse(currentNode, Set()).toList
          val newNodesWithBranches = newNodes.map { NodeWithBranch(_, nextBranch) }

          val newPendingNodes: List[NodeWithBranch[N]] = newNodesWithBranches ::: pendingNodes
          innerHasCycles(newPendingNodes)
        }
    }

    innerHasCycles(List(NodeWithBranch(initialNode, List())))
  }
}
