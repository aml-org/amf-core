package amf.core.internal.parser.domain

import amf.core.client.scala.model._
import amf.core.client.scala.model.domain._
import amf.core.client.scala.vocabulary.ValueType
import amf.core.internal.metamodel.Type._
import amf.core.internal.metamodel.{Field, Obj, Type}
import org.mulesoft.common.time.SimpleDateTime

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** Field values
  */
class Fields {

  private var fs: Map[Field, Value] = ListMap()

  def default(field: Field): AmfElement =
    Option(field.`type`).filter(_.isInstanceOf[Array]).map(_ => AmfArray(Nil)).orNull

  /** Return typed value associated to given Field. */
  def get(field: Field): AmfElement = {
    getValue(field) match {
      case Value(value, _) => value
      case _               => default(field)
    }
  }

  def field[T](f: Field): T = {

    def typed(t: Type, e: AmfElement): Any = e match {
      case s: AmfScalar =>
        t match {
          case Str | Iri                 => new StrFieldImpl(s, f)
          case Bool                      => new BoolFieldImpl(s, f)
          case Type.Int                  => new IntFieldImpl(s, f)
          case Type.Float                => new DoubleFieldImpl(s, f)
          case Type.Double               => new DoubleFieldImpl(s, f)
          case Type.Any                  => new AnyFieldImpl(s, f)
          case Type.Date | Type.DateTime => new DateFieldImpl(s, f)
          case _                         => throw new Exception(s"Invalid value '$s' of type '$t'")
        }
      case o: AmfObject =>
        t match {
          case _: Obj => o
          case _      => throw new Exception(s"Invalid value '$o' of type '$t'")
        }
      case a: AmfArray =>
        t match {
          case ArrayLike(element) => a.values.map(typed(element, _))
          case _                  => throw new Exception(s"Invalid value '$a' of type '$t'")
        }
    }

    def empty(): T =
      (f.`type` match {
        case Str | Iri    => StrFieldImpl(f.defaultValue.asInstanceOf[Option[String]], Annotations(), f)
        case Bool         => BoolFieldImpl(f.defaultValue.asInstanceOf[Option[Boolean]], Annotations(), f)
        case Type.Int     => IntFieldImpl(f.defaultValue.asInstanceOf[Option[Int]], Annotations(), f)
        case Type.Float   => DoubleFieldImpl(f.defaultValue.asInstanceOf[Option[Double]], Annotations(), f)
        case Type.Double  => DoubleFieldImpl(f.defaultValue.asInstanceOf[Option[Double]], Annotations(), f)
        case Type.Any     => AnyFieldImpl(f.defaultValue, Annotations(), f)
        case ArrayLike(_) => Nil
        case Type.Date | Type.DateTime =>
          DateFieldImpl(f.defaultValue.asInstanceOf[Option[SimpleDateTime]], Annotations(), f)
        case _: Obj => null
      }).asInstanceOf[T]

    fs.get(f).map(v => typed(f.`type`, v.value)).fold(empty())(_.asInstanceOf[T])
  }

  def fieldsMeta(): List[Field] = fs.keys.toList

  def ?[T](field: Field): Option[T] = fs.get(field).map(_.value.asInstanceOf[T])

  /** Return [[Value]] associated to given Field. */
  def getValue(field: Field): Value = fs.get(field).orNull

  def getValueAsOption(iri: String): Option[Value] = {
    val constructedField = Field(Type.Any, ValueType(iri)) // valueType iri is the only relevant value
    getValueAsOption(constructedField)
  }

  /** Return [[Value]] associated to given Field. */
  def getValueAsOption(field: Field): Option[Value] = fs.get(field)

  /** Add field array - value. */
  def add(id: String, field: Field, value: AmfElement): this.type = {
    adopt(id, value)
    ?[AmfArray](field) match {
      case Some(array) =>
        array += value
        this
      case None => set(id, field, AmfArray(Seq(value)))
    }
  }

  /** Add field array - value. */
  def addWithoutId(field: Field, value: AmfElement): this.type = {
    // adopt(id, value)
    ?[AmfArray](field) match {
      case Some(array) =>
        array += value
        this
      case None => setWithoutId(field, AmfArray(Seq(value)))
    }
  }

  /** Set field value entry-point. */
  def set(id: String, field: Field, value: AmfElement, annotations: Annotations = Annotations()): this.type = {
    if (field.value.iri() == "http://a.ml/vocabularies/document#declares") {
      // declaration, set correctly the id
      adopt(id + "#/declarations", value)
    } else {
      adopt(id, value)
    }
    fs = fs + (field -> Value(value, annotations))
    this
  }

  /** Set field value entry-point without adopting it. */
  def setWithoutId(field: Field, value: AmfElement, annotations: Annotations = Annotations()): this.type = {
    fs = fs + (field -> Value(value, annotations))
    this
  }

  def link(field: Field, value: AmfObject, annotations: Annotations): this.type = {
    fs = fs + (field -> Link(value, annotations))
    this
  }

  def removeField(field: Field): this.type = {
    fs = fs - field
    this
  }

  def remove(uri: String): this.type = {
    fs.find(t => t._1.value.iri().equals(uri)).foreach(t => removeField(t._1))
    this
  }

  def overrideWith(other: Fields): Unit = {
    fs = other.fs
  }

  def into(other: Fields): Unit = {
    // TODO array copy with references instead of instance
    other.fs = other.fs ++ fs
  }

  def apply[T](field: Field): T = rawAny(get(field))

  def raw[T](field: Field): Option[T] = getValue(field) match {
    case Value(value, _) => Some(rawAny(value))
    case _               => None
  }

  private def rawAny[T](element: AmfElement): T = {
    (element match {
      case AmfArray(values, _) => values.map(rawAny[T])
      case AmfScalar(value, _) => value
      case obj                 => obj
    }).asInstanceOf[T]
  }

  /** Return optional entry for a given Field. */
  def entry(f: Field): Option[FieldEntry] = {
    fs.get(f) match {
      case Some(value) => Some(FieldEntry(f, value))
      case _           => None
    }
  }

  /** Return if the given Field exists within this [[Fields]] instance. */
  def exists(f: Field): Boolean = fs.contains(f)

  def entryJsonld(f: Field): Option[FieldEntry] = {
    if (f.jsonldField) {
      entry(f)
    } else {
      None
    }
  }

  def foreach(fn: ((Field, Value)) => Unit): Unit = {
    fs.foreach(fn)
  }

  def filter(fn: ((Field, Value)) => Boolean): Fields = {
    fs = fs.filter(fn)
    this
  }

  def fieldsValues(): Iterable[Value] = fs.values

  def adopt(id: String, value: AmfElement): Unit = value match {
    case obj: AmfObject => obj.adopted(id)
    case seq: AmfArray  => seq.values.foreach(adopt(id, _))
    case _              => // Do nothing with scalars
  }

  def fields(): Iterable[FieldEntry] = fs.map(FieldEntry.tupled)

  def size: Int = fs.size

  def nonEmpty: Boolean = fs.nonEmpty

  sealed trait FieldRemover {
    val field: Field
    def remove(): Unit = removeField(field)
  }
  private case class StrFieldImpl(option: Option[String], annotations: Annotations, field: Field)
      extends StrField
      with FieldRemover {
    def this(s: AmfScalar, f: Field) = this(Option(s.value).map(_.asInstanceOf[String]), s.annotations, f)
  }

  private case class BoolFieldImpl(option: Option[Boolean], annotations: Annotations, field: Field)
      extends BoolField
      with FieldRemover {
    def this(s: AmfScalar, f: Field) = this(Option(s.value).map(_.asInstanceOf[Boolean]), s.annotations, f)
  }

  private case class IntFieldImpl(option: Option[Int], annotations: Annotations, field: Field)
      extends IntField
      with FieldRemover {
    def this(s: AmfScalar, f: Field) = this(Option(s.value).map(_.asInstanceOf[Int]), s.annotations, f)
  }

  private case class DoubleFieldImpl(option: Option[Double], annotations: Annotations, field: Field)
      extends DoubleField
      with FieldRemover {
    def this(s: AmfScalar, f: Field) = this(s.toNumberOption.map(_.asInstanceOf[Double]), s.annotations, f)
  }

  private case class DateFieldImpl(option: Option[SimpleDateTime], annotations: Annotations, field: Field)
      extends DateField
      with FieldRemover {
    def this(s: AmfScalar, f: Field) = this(SimpleDateTime.unapply(s.value.toString), s.annotations, f)
  }

  private case class AnyFieldImpl(option: Option[Any], annotations: Annotations, field: Field)
      extends AnyField
      with FieldRemover {
    def this(s: AmfScalar, f: Field) = this(Option(s.value), s.annotations, f)
  }

  def copy(): Fields = {
    val copied = new Fields()
    fs.foreach { copied.fs += _ }
    copied
  }

  private[amf] def cloneFields(branch: mutable.Map[AmfObject, AmfObject]): Fields = {
    val cloned = new Fields()
    fs.foreach { case (field, value) => cloned.fs += (field -> value.cloneValue(branch)) }
    cloned
  }
}

object Fields {
  def apply(): Fields = new Fields()
}

class Value(var value: AmfElement, val annotations: Annotations) {

  // Values are going to *mutate* automatically when references to unresolved values are resolved in declarations
  checkUnresolved()

  override def toString: String = value.toString

  def checkUnresolved(): Unit = {
    value match {
      case linkable: Linkable if linkable.isUnresolved =>
        // this is a callback that will be registered
        // in the declarations of the parser context
        // to be executed when a reference is resolved
        linkable.toFutureRef((resolved) => {
          if (linkable.isLink) {
            value = resolved
          } else {
            value = resolved.resolveUnreferencedLink(
              linkable.refName,
              linkable.annotations,
              linkable,
              linkable.supportsRecursion.option().getOrElse(false)
            ) // mutation of the field value
          }
          val syntax = value match {
            case s: Shape => Some(s.ramlSyntaxKey)
            case _        => None
          }
          linkable.afterResolve(syntax, resolved.id) // triggers the after resolve logic
        })

      case array: AmfArray => // Same for arrays, but iterating through elements and looking for unresolved
        array.values.foreach {
          case linkable: Linkable if linkable.isUnresolved =>
            linkable.toFutureRef((resolved) => {
              val unresolved = ListBuffer[(Linkable, Option[String])]()
              value.asInstanceOf[AmfArray].values = value.asInstanceOf[AmfArray].values map { element =>
                if (element == linkable) {
                  val syntax = resolved match {
                    case s: Shape => Some(s.ramlSyntaxKey)
                    case _        => None
                  }
                  unresolved += (element
                    .asInstanceOf[Linkable] -> syntax) // we need to collect the linkables unresolved instances, to run the after resolve trigger. This will end the father parser logic when its necessary
                  resolved.resolveUnreferencedLink(
                    linkable.refName,
                    linkable.annotations,
                    element,
                    linkable.supportsRecursion.option().getOrElse(false)
                  )
                } else {
                  element
                }
              }
              // we need to wait until the field inherits of father is mutated, so we can triggers the after resolve parsing with the instance totally parser.If we trigger in the resolve unreferenced link, the value of the father field it would not have changed yet.
              unresolved
                .foreach { case (ur, key) =>
                  ur.afterResolve(key, resolved.id)
                } // triggers the after resolved logic in all unresolve collected linkables instances.
            })

          case _ => // ignore
        }
      case _ => // ignore
    }
  }

  def cloneAnnotated(annotation: Annotation) = Value(value, Annotations(annotations))

  def cloneValue(branch: mutable.Map[AmfObject, AmfObject]): Value = {
    val clonedElement     = value.cloneElement(branch)
    val clonedAnnotations = annotations.copy()
    Value(clonedElement, clonedAnnotations)
  }

  lazy val isInferred: Boolean = annotations.isInferred

  lazy val isSynthesized: Boolean = annotations.isSynthesized
}

object Value {
  def apply(value: AmfElement, annotations: Annotations) = new Value(value, annotations)

  def unapply(link: Value): Option[(AmfElement, Annotations)] =
    if (Option(link).isDefined)
      Some((link.value, link.annotations))
    else
      None
}

class Link(value: AmfObject, annotations: Annotations) extends Value(value, annotations) {
  override def toString: String = value.id
}

object Link {
  def apply(value: AmfObject, annotations: Annotations) = new Link(value, annotations)
}

case class FieldEntry(field: Field, value: Value) {
  def element: AmfElement = value.value

  def scalar: AmfScalar = element.asInstanceOf[AmfScalar]

  def array: AmfArray = element.asInstanceOf[AmfArray]

  def arrayValues[T <: AmfElement]: Seq[T] = array.values.map(_.asInstanceOf[T])

  def arrayValues[T <: AmfElement](clazz: Class[T]): Seq[T] = arrayValues

  def obj: AmfObject = element.asInstanceOf[AmfObject]

  def negated: FieldEntry = copy(value = Value(AmfScalar(!scalar.toBool, element.annotations), value.annotations))

  def domainElement: DomainElement = element.asInstanceOf[DomainElement]

  def isLink: Boolean = value.isInstanceOf[Link]
}
