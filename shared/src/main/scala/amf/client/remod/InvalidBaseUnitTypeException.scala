package amf.client.remod

import amf.core.metamodel.Obj

case class InvalidBaseUnitTypeException(currentIri: String, exceptedIri: String)
    extends Exception(s"Current base unit of type $currentIri is not of expected type $exceptedIri") {}

object InvalidBaseUnitTypeException {
  def forMeta(current: Obj, expected: Obj): InvalidBaseUnitTypeException = {
    InvalidBaseUnitTypeException(current.`type`.headOption.map(_.iri()).getOrElse(""), expected.`type`.head.iri())
  }
}
