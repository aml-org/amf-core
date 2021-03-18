package amf.client.remod

import scala.collection.GenTraversableLike
import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag

package object internal {

  implicit case class FilterType[T[A] <: GenTraversableLike[A, T[A]], A](t: T[A]) {
    def filterByType[B <: A](implicit tag: ClassTag[B], bf: CanBuildFrom[T[A], B, T[B]]): T[B] = t.collect {
      case element: B => element
    }
  }
}
