package amf.core.internal.parser.domain

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.Fragment
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.model.domain.extensions.CustomDomainProperty
import amf.core.internal.utils.QName
import amf.core.internal.parser.domain.SearchScope.{All, Fragments, Named}
import amf.core.internal.validation.CoreValidations.DeclarationNotFound
import org.mulesoft.lexer.SourceLocation

class Declarations(var libraries: Map[String, Declarations] = Map(),
                   var fragments: Map[String, FragmentRef] = Map(),
                   var annotations: Map[String, CustomDomainProperty] = Map(),
                   errorHandler: AMFErrorHandler,
                   futureDeclarations: FutureDeclarations) {

  var promotedFragments: Seq[Fragment] = Seq[Fragment]()

  def +=(fragment: (String, Fragment)): Declarations = {
    fragment match {
      case (url, f) => fragments = fragments + (url -> FragmentRef(f.encodes, f.location()))
    }
    this
  }

  def +=(element: DomainElement): Declarations = {
    element match {
      case a: CustomDomainProperty =>
        annotations = annotations + (a.name.value() -> a)
    }
    this
  }

  /** Find domain element with the same name. */
  def findEquivalent(element: DomainElement): Option[DomainElement] = element match {
    case a: CustomDomainProperty => findAnnotation(a.name.value(), SearchScope.All)
    case _                       => None
  }

  /** Get or create specified library. */
  def getOrCreateLibrary(alias: String): Declarations = {
    libraries.get(alias) match {
      case Some(lib) => lib
      case None =>
        val result = new Declarations(errorHandler = errorHandler, futureDeclarations = futureDeclarations)
        libraries = libraries + (alias -> result)
        result
    }
  }

  protected def error(message: String, pos: SourceLocation): Unit =
    errorHandler.violation(DeclarationNotFound, "", message, pos)

  def declarables(): Seq[DomainElement] =
    annotations.values.toSeq

  def findAnnotationOrError(pos: SourceLocation)(key: String, scope: SearchScope.Scope): CustomDomainProperty =
    findAnnotation(key, scope) match {
      case Some(result) => result
      case _ =>
        error(s"Annotation '$key' not found", pos)
        ErrorCustomDomainProperty
    }

  def findAnnotation(key: String, scope: SearchScope.Scope): Option[CustomDomainProperty] =
    findForType(key, _.annotations, scope) collect {
      case a: CustomDomainProperty => a
    }

  def findForType(key: String,
                  map: Declarations => Map[String, DomainElement],
                  scope: SearchScope.Scope): Option[DomainElement] = {
    def inRef(): Option[DomainElement] = {
      val fqn = QName(key)
      val result = if (fqn.isQualified) {
        libraries.get(fqn.qualification).flatMap(_.findForType(fqn.name, map, scope))
      } else None

      result
        .orElse {
          map(this).get(key)
        }
    }

    scope match {
      case All       => inRef().orElse(fragments.get(key).map(_.encoded))
      case Fragments => fragments.get(key).map(_.encoded)
      case Named     => inRef()
    }
  }

  trait ErrorDeclaration

  object ErrorCustomDomainProperty extends CustomDomainProperty(Fields(), Annotations()) with ErrorDeclaration

}

case class FragmentRef(encoded: DomainElement, location: Option[String])

object FragmentRef {
  def apply(f: Fragment): FragmentRef = new FragmentRef(f.encodes, f.location())
}

object Declarations {

  def apply(declarations: Seq[DomainElement],
            errorHandler: AMFErrorHandler,
            futureDeclarations: FutureDeclarations): Declarations = {
    val result = new Declarations(errorHandler = errorHandler, futureDeclarations = futureDeclarations)
    declarations.foreach(result += _)
    result
  }
}

object SearchScope {
  trait Scope

  object All       extends Scope
  object Fragments extends Scope
  object Named     extends Scope
}
