package amf.core.client.scala.model.domain

import amf.core.internal.parser.domain.Annotations

import scala.collection.mutable

/** Created by pedro.colunga on 8/15/17.
  */
case class AmfArray(var values: Seq[AmfElement], annotations: Annotations = new Annotations()) extends AmfElement {

  def +=(value: AmfElement): Unit = {
    values = values :+ value
  }

  def scalars: Seq[AmfScalar] = values collect { case s: AmfScalar => s }

  override def cloneElement(branch: mutable.Map[AmfObject, AmfObject]): AmfArray =
    AmfArray(values.map(_.cloneElement(branch)), annotations.copy())
}
