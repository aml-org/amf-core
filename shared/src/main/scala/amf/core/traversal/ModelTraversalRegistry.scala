package amf.core.traversal

import amf.core.annotations.TypeAlias
import amf.core.model.domain.{RecursiveShape, Shape}

import scala.collection.mutable

case class ModelTraversalRegistry() {

  // All IDs visited in the traversal
  private var visitedIds: mutable.Set[String] = mutable.Set()

  // IDs visited in the current path
  private var currentPath: Set[String] = Set.empty

  // IDs of elements that do not throw recursion errors
  private var allowedList: Set[String] = Set()

  private var allowedCycleClasses: Seq[Class[_]] = Seq()

  // Function that skips elements
  private var skipFn: String => Boolean = (_: String) => false

  def withSkipFn(fn: String => Boolean): this.type = {
    skipFn = fn
    this
  }

  def withAllowedCyclesInstances(classes: Seq[Class[_]]): this.type = {
    allowedCycleClasses = classes
    this
  }

  def resetSkipFn(): this.type = {
    skipFn = (_: String) => false
    this
  }

  def +(id: String): this.type = {
    visitedIds += id
    currentPath += id
    this
  }

  def shouldFailIfRecursive(root: Shape, shape: Shape): Boolean = root.annotations.find(classOf[TypeAlias]) match {
    case Some(alias) =>
      val a = (alias.aliasId.equals(shape.id) && currentPath.nonEmpty || isInCurrentPath(shape.id))
      val b = !isAllowedToCycle(shape)
      a && b
    case None => isInCurrentPath(shape.id) && !isAllowedToCycle(shape)
  }

  def avoidError(id: String): Boolean = allowedList.contains(id)

  def avoidError(r: RecursiveShape, checkId: Option[String] = None): Boolean =
    avoidError(r.id) || avoidError(r.fixpoint.option().getOrElse("")) || (checkId.isDefined && avoidError(checkId.get))

  def isInCurrentPath(id: String): Boolean = currentPath.contains(id)

  def isAllowedToCycle(shape: Shape): Boolean = allowedCycleClasses.contains(shape.getClass)

  def canTraverse(id: String): Boolean = !skipFn(id)

  def runWithIgnoredId(fnc: () => Shape, shapeId: String): Shape = runWithIgnoredIds(fnc, Set(shapeId))

  def runWithIgnoredIds(fnc: () => Shape, shapeIds: Set[String]): Shape = {
    val previousAllowedList = allowedList
    allowedList = allowedList ++ shapeIds
    val expanded = runNested(_ => fnc())
    allowedList = previousAllowedList
    expanded
  }

  def recursionAllowed(fnc: () => Shape, shapeId: String): Shape = {
    val actual = currentPath + shapeId
    runWithIgnoredIds(fnc, actual)
  }

  def wasVisited(id: String): Boolean = visitedIds.contains(id)

  // Runs a function and restores the currentPath to its original state after the run
  def runNested[T](fnc: ModelTraversalRegistry => T): T = {
    val previousPath = currentPath
    val element      = fnc(this)
    currentPath = previousPath
    element
  }

}
