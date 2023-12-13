package amf.core.internal.remote.platform

import amf.core.client.platform.model.AmfObjectWrapper
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfObject, DomainElement}
import amf.core.internal.metamodel.Obj

import scala.collection.mutable

trait PlatformWrapperOps {

  private val wrappersRegistry: mutable.HashMap[String, AmfObject => AmfObjectWrapper]           = mutable.HashMap.empty
  private val wrappersRegistryFn: mutable.HashMap[Obj => Boolean, AmfObject => AmfObjectWrapper] = mutable.HashMap.empty

  def registerWrapper(model: Obj)(builder: (AmfObject) => AmfObjectWrapper): Option[AmfObject => AmfObjectWrapper] =
    wrappersRegistry.put(model.`type`.head.iri(), builder)

  def registerWrapperPredicate(p: (Obj) => Boolean)(
      builder: (AmfObject) => AmfObjectWrapper
  ): Option[AmfObject => AmfObjectWrapper] =
    wrappersRegistryFn.put(p, builder)

  def wrap[T <: AmfObjectWrapper](entity: AmfObject): T = entity match {
    case e: DomainElement =>
      wrappersRegistry.get(e.meta.`type`.head.iri()) match {
        case Some(builder) => builder(entity).asInstanceOf[T]
        case None          => wrapFn(e)
      }
    case d: BaseUnit =>
      wrappersRegistry.get(d.meta.`type`.head.iri()) match {
        case Some(builder) => builder(entity).asInstanceOf[T]
        case None          => wrapFn(d)
      }
    case o: AmfObject if wrappersRegistry.contains(o.meta.`type`.head.iri()) =>
      val builder = wrappersRegistry(o.meta.`type`.head.iri())
      builder(entity).asInstanceOf[T]
    case null => null.asInstanceOf[T] // TODO solve this in a better way
    case _    => wrapFn(entity)
  }

  def wrapFn[T <: AmfObjectWrapper](entity: AmfObject): T = entity match {
    case e: DomainElement =>
      wrappersRegistryFn.keys.find(p => p(e.meta)) match {
        case Some(k) => wrappersRegistryFn(k)(e).asInstanceOf[T]
        case None => {
          throw new Exception(s"Cannot find builder for object meta ${e.meta}")
        }
      }
    case d: BaseUnit =>
      wrappersRegistryFn.keys.find(p => p(d.meta)) match {
        case Some(k) => wrappersRegistryFn(k)(d).asInstanceOf[T]
        case None    => throw new Exception(s"Cannot find builder for object meta ${d.meta}")
      }
    case _ => throw new Exception(s"Cannot build object of type $entity")
  }
}
