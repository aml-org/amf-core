package amf.model

import amf.core.model.document.{Fragment => CoreFragment}
import amf.model.domain.{AnnotationTypeDeclaration, ExternalFragment, ResourceTypeFragment, SecuritySchemeFragment, TraitFragment, _}
import amf.plugins.document.webapi.model.{AnnotationTypeDeclarationFragment => CoreAnnotationTypeDeclarationFragment, DataTypeFragment => CoreDataTypeFragment, DialectFragment => CoreDialectFragment, DocumentationItemFragment => CoreDocumentationItemFragment, ExternalFragment => CoreExternalFragment, NamedExampleFragment => CoreNamedExampleFragment, ResourceTypeFragment => CoreResourceTypeFragment, SecuritySchemeFragment => CoreSecuritySchemeFragment, TraitFragment => CoreTraitFragment}

/**
  * JVM Fragment model class
  */
abstract class Fragment(private[amf] val fragment: CoreFragment) extends BaseUnit with EncodesModel {

  override private[amf] val element = fragment

}

object Fragment {
  def apply(fragment: CoreFragment): domain.Fragment = fragment match {
    case a: CoreAnnotationTypeDeclarationFragment => AnnotationTypeDeclaration(a)
    case d: CoreDataTypeFragment                  => DataType(d)
    case d: CoreDialectFragment                   => DialectFragment(d)
    case d: CoreDocumentationItemFragment         => DocumentationItem(d)
    case e: CoreExternalFragment                  => ExternalFragment(e)
    case n: CoreNamedExampleFragment              => NamedExample(n)
    case r: CoreResourceTypeFragment              => ResourceTypeFragment(r)
    case s: CoreSecuritySchemeFragment            => SecuritySchemeFragment(s)
    case t: CoreTraitFragment                     => TraitFragment(t)
  }
}

case class DocumentationItem(private[amf] val documentationItem: CoreDocumentationItemFragment)
  extends domain.Fragment(documentationItem) {
  def this() = this(CoreDocumentationItemFragment())

}

case class DataType(private[amf] val dataType: CoreDataTypeFragment) extends domain.Fragment(dataType) {
  def this() = this(CoreDataTypeFragment())
}

case class NamedExample(private[amf] val namedExample: CoreNamedExampleFragment)
  extends domain.Fragment(namedExample) {
  def this() = this(CoreNamedExampleFragment())
}

case class DialectFragment(private[amf] val df: CoreDialectFragment) extends domain.Fragment(df) {
  def this() = this(CoreDialectFragment())
}

case class ExternalFragment(private[amf] val ef: CoreExternalFragment) extends domain.Fragment(ef) {
  def this() = this(CoreExternalFragment())
}

case class ResourceTypeFragment(private[amf] val resourceTypeFragment: CoreResourceTypeFragment)
  extends domain.Fragment(resourceTypeFragment) {

  def this() = this(CoreResourceTypeFragment())
}

case class TraitFragment(private[amf] val traitFragment: CoreTraitFragment)
  extends domain.Fragment(traitFragment) {

  def this() = this(CoreTraitFragment())
}

case class AnnotationTypeDeclaration(
                                      private[amf] val annotationTypeDeclaration: CoreAnnotationTypeDeclarationFragment)
  extends domain.Fragment(annotationTypeDeclaration) {
  def this() = this(CoreAnnotationTypeDeclarationFragment())
}

case class SecuritySchemeFragment(private[amf] val extensionFragment: CoreSecuritySchemeFragment)
  extends Fragment(extensionFragment) {

  def this() = this(CoreSecuritySchemeFragment())
}
