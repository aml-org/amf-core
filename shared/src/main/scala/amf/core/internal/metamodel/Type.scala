package amf.core.internal.metamodel

import amf.core.internal.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.client.scala.vocabulary.Namespace.Xsd
import amf.core.client.scala.vocabulary.ValueType

import scala.collection.immutable

/**
  *
  */
trait Type {
  val `type`: List[ValueType]
  def typeIris: List[String] = `type`.map(_.iri())
}

trait Obj extends Type {

  // This can be override by dynamic element models to provide
  // the information about fields at parsing time

  val doc: ModelDoc = ModelDoc()

  def fields: List[Field]
}

object Type {

  case class Scalar(id: String) extends Type {
    override val `type`: List[ValueType] = List(Xsd + id)
  }

  object Str extends Scalar("string")

  object RegExp extends Scalar("regexp")

  object Int extends Scalar("int")

  object Float extends Scalar("float")

  object Double extends Scalar("double")

  object Time extends Scalar("time")

  object Date extends Scalar("date")

  object DateTime extends Scalar("dateTime")

  object Iri extends Scalar("url")

  object EncodedIri extends Scalar("encodedUrl")

  object LiteralUri extends Scalar("literalUri")

  object Bool extends Scalar("boolean")

  object ObjType extends Obj {
    override val fields: List[Field]     = Nil
    override val `type`: List[ValueType] = Nil

    override val doc: ModelDoc = ModelDoc(ModelVocabularies.Parser, "", "")
  }

  abstract class ArrayLike(val element: Type) extends Type {
    override val `type`: List[ValueType] = element.`type`
  }

  object ArrayLike {
    def unapply(arg: ArrayLike): Option[Type] = Some(arg.element)
  }

  case class Array(override val element: Type) extends ArrayLike(element)

  case class SortedArray(override val element: Type) extends ArrayLike(element)

  object Any extends Type {
    override val `type`: List[ValueType] = List(Xsd + "anyType")
  }

}
