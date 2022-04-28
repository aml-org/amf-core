package amf.core.client.scala.parse

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.Reference
import amf.core.internal.annotations.ReferenceTargets
import amf.core.internal.parser.Root
import org.mulesoft.common.client.lexical.PositionRange

object TaggedReferences {

  implicit class BuReferenceTagger(baseUnit: BaseUnit) {
    def tagReferences(root: Root): BaseUnit = {
      val t = root.references.flatMap { r =>
        taggedReferences(r.unit.location().getOrElse(r.unit.id), r.origin)
      }.toMap
      val targets = ReferenceTargets(t)
      baseUnit.add(targets)
    }

    def tagReference(location: String, r: Reference): BaseUnit = {
      val rta: ReferenceTargets = baseUnit.annotations.find(classOf[ReferenceTargets]) match {
        case Some(rt) =>
          baseUnit.annotations.reject(_.isInstanceOf[ReferenceTargets])
          rt ++ taggedReferences(location, r)
        case _ => ReferenceTargets(taggedReferences(location, r))
      }
      baseUnit.add(rta)
    }

  }

  def taggedReferences(location: String, r: Reference): Map[String, Seq[PositionRange]] =
    Map(location -> r.refs.map(_.reduceToLocation()))
}
