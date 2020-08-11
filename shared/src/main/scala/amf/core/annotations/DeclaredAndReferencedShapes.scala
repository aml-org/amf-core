package amf.core.annotations

import amf.core.metamodel.domain.DomainElementModel
import amf.core.model.domain.Annotation

/**
  * Used to maintain shapes that where declarations or references, used for optimizing emission.
  * These shapes may latter be extracted to declares arrray in jsonld and avoid multiple emissions.
  * These annotations must not be serialized into jsonld.
  */
case class References(references: Seq[String]) extends Annotation
case class Declares(declares: Seq[String]) extends Annotation
case class DeclarationKeys(keys: List[DeclarationKey]) extends Annotation
case class DeclarationKey(model: DomainElementModel, lexical: LexicalInformation, displayName: Option[String] = None)

object DeclarationKey {
  def apply(model: DomainElementModel, lexical: LexicalInformation, displayName: Option[String] = None): DeclarationKey = new DeclarationKey(model, lexical, displayName)

  def apply(model: DomainElementModel, lexical: LexicalInformation, displayName: String): DeclarationKey = DeclarationKey(model, lexical, Some(displayName))
}