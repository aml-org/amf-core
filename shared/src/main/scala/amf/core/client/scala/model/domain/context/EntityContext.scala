package amf.core.client.scala.model.domain.context

import amf.core.client.scala.vocabulary.ValueType
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.DomainElementModel

import scala.collection.mutable

class EntityContext(val entities: List[EntityModel]) {}

case class EntityModel(`type`: ValueType, properties: Map[ValueType, ValueType])

class EntityContextBuilder {
  def build(): EntityContext = new EntityContext(
    entities
      .map({ case (uri, list) => EntityModel(uri, list.map(f => f.value -> f.`type`.`type`.head).toMap) })
      .toList
  )

  val entities: mutable.Map[ValueType, Set[Field]] = mutable.Map()

  def +(jsonLDElementModel: DomainElementModel): EntityContextBuilder = {
    entities.get(jsonLDElementModel.`type`.head) match {
      case Some(l) => entities.update(jsonLDElementModel.`type`.head, jsonLDElementModel.fields.toSet ++ l)
      case _       => entities.put(jsonLDElementModel.`type`.head, jsonLDElementModel.fields.toSet)
    }
    this
  }
}

trait SelfContainedContext {
  val entityContext: EntityContext
}
