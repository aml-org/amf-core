package amf.core.client.scala.validation

import amf.core.client.common.position.{Position, Range}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfObject, DomainElement}
import amf.core.internal.annotations.{LexicalInformation, SourceLocation}
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.validation.core.ValidationResult

import java.io.StringWriter
import java.util.Objects

case class AMFValidationResult private[amf] (message: String,
                                             severityLevel: String,
                                             private val targetNodeValue: Either[AmfObject, String],
                                             targetProperty: Option[String], // TODO: is it used??
                                             validationId: String,
                                             position: Option[LexicalInformation],
                                             location: Option[String],
                                             source: Any)
    extends Ordered[AMFValidationResult] {

  def targetNode: String =
    targetNodeValue match {
      case Left(obj) => obj.id
      case Right(id) => id
    }

  override def toString: String = AMFValidationReportPrinter.print(this)

  override def equals(obj: Any): Boolean = obj match {
    case other: AMFValidationResult =>
      other.message.equals(message) &&
        other.validationId == validationId &&
        other.location.getOrElse("") == location.getOrElse("") &&
        samePosition(other.position)
    case _ => false
  }

  override def hashCode(): Int = {
    Objects.hash(message, validationId, location.getOrElse(""), position.map(_.range.toString).getOrElse(""))
  }

  private def samePosition(otherPosition: Option[LexicalInformation]): Boolean = {
    otherPosition match {
      case Some(otherPos) if position.isDefined =>
        val pos = position.get
        otherPos.range.toString.equals(pos.range.toString)
      case None => position.isEmpty
      case _    => false
    }
  }

  override def compare(that: AMFValidationResult): Int = {

    val thatPosition = if (that.position != null) that.position else None
    val thisPosition = if (this.position != null) this.position else None
    val i = thisPosition
      .map(_.range.start)
      .getOrElse(Position(0, 0)) compareTo thatPosition.map(_.range.start).getOrElse(Position(0, 0)) match {
      case 0 =>
        thisPosition
          .map(_.range.end)
          .getOrElse(Position(0, 0)) compareTo thatPosition.map(_.range.end).getOrElse(Position(0, 0)) match {
          case 0 =>
            this.targetProperty.getOrElse("") compareTo that.targetProperty.getOrElse("") match {
              case 0 =>
                Option(this.targetNode).getOrElse("") compareTo Option(that.targetNode).getOrElse("") match {
                  case 0 =>
                    Option(this.validationId).getOrElse("") compareTo Option(that.validationId).getOrElse("") match {
                      case 0 =>
                        Option(this.message).getOrElse("") compareTo Option(that.message).getOrElse("")
                      case x => x
                    }
                  case x => x
                }
              case x => x
            }
          case x => x
        }
      case x => x
    }
    if (i > 0) 1
    else if (i == 0) i
    else -1
  }

  val completeMessage: String = {
    val str = StringBuilder.newBuilder
    str.append(s"\n- Source: $validationId\n")
    str.append(s"  Message: $message\n")
    str.append(s"  Property: ${targetProperty.getOrElse("")}\n")
    str.toString
  }
}

object AMFValidationResult {

  def apply(message: String,
            level: String,
            targetNode: String,
            targetProperty: Option[String],
            validationId: String,
            position: Option[LexicalInformation],
            location: Option[String],
            source: Any): AMFValidationResult =
    new AMFValidationResult(message,
                            level,
                            Right(targetNode),
                            targetProperty,
                            validationId,
                            position,
                            location,
                            source)

  def apply(message: String,
            level: String,
            targetNode: AmfObject,
            targetProperty: Option[String],
            validationId: String,
            position: Option[LexicalInformation],
            location: Option[String],
            source: Any): AMFValidationResult =
    new AMFValidationResult(message, level, Left(targetNode), targetProperty, validationId, position, location, source)

  def fromSHACLValidation(model: BaseUnit,
                          message: String,
                          level: String,
                          validation: ValidationResult): AMFValidationResult = {
    model.findById(validation.focusNode) match {
      case None if validation.focusNode == model.id =>
        AMFValidationResult(
            message = message,
            level = level,
            targetNode = model.id,
            targetProperty = Option(validation.path),
            validation.sourceShape,
            position = Some(LexicalInformation.apply(Range.NONE)),
            location = model.location(),
            source = validation
        )
      case None => throw new Exception(s"Cannot find node with validation error ${validation.focusNode}")
      case Some(node) =>
        val (pos, location) = findPositionAndLocation(node, validation)
        AMFValidationResult(
            message = message,
            level = level,
            targetNode = node.id,
            targetProperty = Option(validation.path),
            validation.sourceShape,
            position = pos,
            location = location,
            source = validation
        )
    }
  }

  def withShapeId(shapeId: String, validation: AMFValidationResult): AMFValidationResult =
    AMFValidationResult(
        validation.message,
        validation.severityLevel,
        validation.targetNode,
        validation.targetProperty,
        shapeId,
        validation.position,
        validation.location,
        validation.source
    )

  private def findPositionAndLocation(node: DomainElement,
                                      validation: ValidationResult): (Option[LexicalInformation], Option[String]) = {

    val annotations: Annotations = if (Option(validation.path).isDefined && validation.path != "") {
      node.fields.fields().find(f => f.field.value.iri() == validation.path) match {
        case Some(f) if f.element.annotations.contains(classOf[LexicalInformation]) => f.element.annotations
        case Some(f) if f.value.annotations.contains(classOf[LexicalInformation])   => f.value.annotations
        case Some(f) =>
          f.element match {
            case arr: AmfArray if arr.values.nonEmpty =>
              arr.values.head.annotations
            case _ => node.annotations
          }
        case _ => node.annotations
      }
    } else {
      node.annotations
    }
    (annotations.find(classOf[LexicalInformation]), annotations.find(classOf[SourceLocation]).map(_.location))
  }
}
