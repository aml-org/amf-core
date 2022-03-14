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

  object Literal {
    def apply(dataType: String): Option[Scalar] = dataType match {
      case _ if dataType == Str.`type`.head.iri()        => Some(Str)
      case _ if dataType == Int.`type`.head.iri()        => Some(Int)
      case _ if dataType == Float.`type`.head.iri()      => Some(Float)
      case _ if dataType == Double.`type`.head.iri()     => Some(Double)
      case _ if dataType == RegExp.`type`.head.iri()     => Some(RegExp)
      case _ if dataType == Time.`type`.head.iri()       => Some(Time)
      case _ if dataType == Date.`type`.head.iri()       => Some(Date)
      case _ if dataType == DateTime.`type`.head.iri()   => Some(DateTime)
      case _ if dataType == Iri.`type`.head.iri()        => Some(Iri)
      case _ if dataType == EncodedIri.`type`.head.iri() => Some(EncodedIri)
      case _ if dataType == LiteralUri.`type`.head.iri() => Some(LiteralUri)
      case _ if dataType == Bool.`type`.head.iri()       => Some(Bool)
      case _                                             => None
    }
  }

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
