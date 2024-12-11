package amf.core.internal.transform.stages.elements.resolution

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.Document
import amf.core.client.scala.model.domain.{DomainElement, LinkNode, Linkable, NamedAmfObject, NamedDomainElement}
import amf.core.client.scala.traversal.{
  DomainElementSelectorAdapter,
  DomainElementTransformationAdapter,
  TransformationData,
  TransformationTraversal
}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.annotations._
import amf.core.internal.metamodel.document.DocumentModel
import amf.core.internal.metamodel.domain.LinkableElementModel._
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.transform.stages.LinkNodeResolutionStage
import amf.core.internal.transform.stages.elements.resolution.ReferenceResolution.{
  Condition,
  VALID_DECLARATION_CONDITION
}
import amf.core.internal.transform.stages.helpers.{LinkNodeResolver, ModelReferenceResolver, ResolvedNamedEntity}
import amf.core.internal.transform.stages.selectors.{LinkNodeSelector, LinkSelector}
import amf.core.internal.validation.CoreValidations.RecursiveShapeSpecification

import scala.annotation.tailrec
import scala.collection.mutable

class ReferenceResolution(
    errorHandler: AMFErrorHandler,
    keepEditingInfo: Boolean = false,
    modelResolver: Option[ModelReferenceResolver] = None,
    cache: mutable.Map[String, DomainElement] = mutable.Map(),
    customDomainElementTransformation: (DomainElement, Linkable) => DomainElement = (d: DomainElement, _: Linkable) => d
) extends ElementStageTransformer[DomainElement] {

  private var isCyclicRefElement: Boolean = false

  override def transform(element: DomainElement, configuration: AMFGraphConfiguration): Option[DomainElement] =
    transform(element, Seq(VALID_DECLARATION_CONDITION), configuration)

  def transform(
      element: DomainElement,
      conditions: Seq[Condition],
      configuration: AMFGraphConfiguration
  ): Option[DomainElement] = {

    // First check cyclic links
    element match {
      case l: Linkable => checkCyclicLinks(l)
      case _           => // Nothing
    }

    element match {
      case l: Linkable if l.isLink =>
        if (cache.contains(l.linkTarget.get.id)) Some(cache(l.linkTarget.get.id))
        else if (cache.contains(element.id) && shouldCopyElement(conditions, l, cache(element.id))) {
          val cached = cache(element.id)
          Some(copyEffectiveLinkTarget(element, cached.asInstanceOf[DomainElement with Linkable]))
        } else {
          val target   = resolveLinkTarget(element, conditions, l)
          var resolved = innerLinkNodeResolution(target, configuration)
          resolved match {
            case linkable: Linkable if l.supportsRecursion.option().getOrElse(false) =>
              linkable.withSupportsRecursion(true)
            case _ => // ignore
          }
          resolved = withName(resolved, l)
          resolved = customDomainElementTransformation(resolved, l)
          resolved.annotations += ResolvedInheritance()
          if (keepEditingInfo) addResolvedLinkAnnotations(l, resolved)
          addIntermediateLinkTargetsToCache(element, resolved)
          resolved = traverseNestedLinksIfCopy(element, conditions, l, resolved, configuration)
          Some(resolved)
        }
      case ln: LinkNode            => LinkNodeResolver.resolveDynamicLink(ln, modelResolver, keepEditingInfo)
      case _ if isCyclicRefElement => Some(element) // If element is self recursive it has the links already removed
      case _                       => None
    }
  }

  @tailrec
  private def checkCyclicLinks(
      next: Linkable,
      stack: Seq[Linkable] = Seq.empty
  ): Unit = {
    if (stack.contains(next)) {
      // found a cycle
      val chain = (stack :+ next).map(displayNameForError).mkString(" -> ")
      errorHandler.violation(RecursiveShapeSpecification, next, s"Invalid cyclic references: $chain", next.annotations)
      isCyclicRefElement = true

      // we do not resolve the cycle, just remove link fields, much like cyclic inheritance
      stack.foreach { e =>
        e.fields.removeField(Target)
        e.fields.removeField(TargetId)
        e.fields.removeField(Label)
      }
    } else {
      next.linkTarget match {
        case Some(newNext: Linkable) => checkCyclicLinks(newNext, stack :+ next)
        case _                       => // Nothing
      }
    }
  }

  /** Link labels are confusing, that's why we prefer using names when possible For example in
    * amf-cli/shared/src/test/resources/validations/oas-definition-self-chained-ref.yaml link labels are shifted a's
    * link label is "b", b's link label is "c" and c's link label is "a"
    */
  def displayNameForError(l: Linkable): String = {
    l match {
      case n: NamedAmfObject => n.name.value()
      case _                 => l.linkLabel.value()
    }
  }

  private def traverseNestedLinksIfCopy(
      element: DomainElement,
      conditions: Seq[Condition],
      l: DomainElement with Linkable,
      resolved: DomainElement,
      configuration: AMFGraphConfiguration
  ) = {
    l.effectiveLinkTarget() match {
      case t: DomainElement with Linkable if shouldCopyElement(conditions, l, t) =>
        cache.put(element.id, resolved)
        resolveNestedLinks(resolved, configuration)
      case _ => resolved
    }
  }

  private def shouldCopyElement(
      conditions: Seq[Condition],
      linkable: Linkable,
      linkableAsElement: DomainElement
  ): Boolean = {
    conditions.forall(condition => condition(linkable, linkableAsElement))
  }

  private def resolveNestedLinks(domainElement: DomainElement, configuration: AMFGraphConfiguration): DomainElement = {
    val selectorAdapter       = new DomainElementSelectorAdapter(LinkSelector || LinkNodeSelector)
    val transformationAdapter = new DomainElementTransformationAdapter((elem, _) => transform(elem, configuration))
    new TransformationTraversal(TransformationData(selectorAdapter, transformationAdapter))
      .traverse(domainElement)
      .asInstanceOf[DomainElement]
  }

  private def addResolvedLinkAnnotations(link: DomainElement with Linkable, resolved: DomainElement) = {
    resolved.annotations += ResolvedLinkAnnotation(link.id)
    link.linkTarget.map { linkTarget =>
      resolved.annotations += ResolvedLinkTargetAnnotation(linkTarget.id)
    }
  }

  private def resolveLinkTarget(element: DomainElement, conditions: Seq[Condition], l: DomainElement with Linkable) = {
    l.effectiveLinkTarget() match {
      case t: DomainElement if cache.contains(t.id) => cache(t.id)
      case t: DomainElement with Linkable if shouldCopyElement(conditions, l, t) =>
        copyEffectiveLinkTarget(element, t)
      case d: DomainElement =>
        propagateAnnotations(l, d)
        d
    }
  }

  private def copyEffectiveLinkTarget(element: DomainElement, resolved: DomainElement with Linkable) = {
    val copiedAnnotations = resolved.annotations.copy().reject(_.isInstanceOf[AutoGeneratedName])
    val copied            = resolved.copyElement(copiedAnnotations).withId(element.id)
    copied.add(TypeAlias(resolved.id))
    copyOriginalName(element, copied)
    copied
  }

  private def copyOriginalName(element: DomainElement, copied: Linkable with DomainElement) = {
    element match {
      case n: NamedDomainElement if n.name.option().isDefined =>
        copied.asInstanceOf[NamedDomainElement].withName(n.name.value(), n.name.annotations())
      case _ => // ignore
    }
  }

  private def propagateAnnotations(link: DomainElement, element: DomainElement): Unit = {
    link.annotations.find(classOf[TrackedElement]).foreach { tracked =>
      addLinkTrackedElementsToResolvedElement(element, tracked, tracked.parents)
    }
    if (hasAutoGeneratedName(link) && !isDeclaredElement(element)) element.add(AutoGeneratedName())
  }

  private def addLinkTrackedElementsToResolvedElement(
      element: DomainElement,
      t: TrackedElement,
      values: Set[String]
  ) = {
    val tracked = element.annotations
      .find(classOf[TrackedElement])
      .fold(t)(inner => TrackedElement(values ++ inner.parents))

    element.annotations.reject(_.isInstanceOf[TrackedElement])
    element.add(tracked)
  }

  // Links traversion to expand annotations and add links to 'cache'
  @scala.annotation.tailrec
  private def addIntermediateLinkTargetsToCache(
      element: DomainElement,
      resolved: DomainElement,
      visited: mutable.Set[String] = mutable.Set()
  ): Unit = {
    if (!visited.contains(element.id)) {
      visited += element.id
      element match {
        case l: Linkable if l.isLink =>
          setDeclaredElementIfMissing(element.annotations, resolved)
          if (isDeclaredElement(element)) cache.put(element.id, resolved)
          addIntermediateLinkTargetsToCache(l.linkTarget.get, resolved, visited)
        case _ => // nothing to do
      }
    }
  }

  private def setDeclaredElementIfMissing(parentAnnotations: Annotations, child: DomainElement): Unit = {
    parentAnnotations.foreach { a =>
      // Only annotation DeclaredElement is added
      if (a.isInstanceOf[DeclaredElement] && !child.annotations.contains(a.getClass)) child.annotations += a
    }
  }

  private def innerLinkNodeResolution(target: DomainElement, configuration: AMFGraphConfiguration): DomainElement = {
    val nested = Document()
    nested.fields.setWithoutId(DocumentModel.Encodes, target)
    val result = new LinkNodeResolutionStage(keepEditingInfo).transform(nested, errorHandler, configuration)
    result.asInstanceOf[Document].encodes
  }

  // TODO: cleanup this and referenced methods. It is not clear in which cases the name is set. Could be extracted to Trait or Object
  private def withName(resolved: DomainElement, source: DomainElement): DomainElement = {
    resolved match {
      case r: NamedDomainElement =>
        if (isExample(r)) {
          source match {
            case s: NamedDomainElement if s.name.option().isDefined => r.withName(s.name.value(), r.name.annotations())
            case _                                                  =>
          }
        } else if (
          r.name.option().isEmpty || r.name.value() == "schema" || r.name.value() == "type" || r.name
            .value() == "body" || r.annotations.contains(classOf[AutoGeneratedName])
        ) {
          source match {
            case s: Linkable => innerName(s, r)
            case _           =>
          }
        }
      case _ =>
    }
    // let's annotate the resolved name
    annotateResolvedName(resolved, source)

    resolved
  }

  private def annotateResolvedName(resolved: DomainElement, source: DomainElement) = {
    def updateRNEAnnotation(name: String, namedDomainElement: NamedDomainElement, rne: ResolvedNamedEntity) = {
      val referenced = rne.vals.getOrElse(name, Nil)
      rne.vals.put(name, referenced :+ namedDomainElement)
    }

    def createOrUpdateRNEAnnotation(name: String, namedDomainElement: NamedDomainElement) = {
      resolved.annotations.find(classOf[ResolvedNamedEntity]) match {
        case Some(rneAnnotation) =>
          updateRNEAnnotation(name, namedDomainElement, rneAnnotation)
        case None =>
          val newRNE = ResolvedNamedEntity()
          updateRNEAnnotation(name, namedDomainElement, newRNE)
          resolved.annotations += newRNE
      }
    }

    source match {
      case s: NamedDomainElement if s.name.nonEmpty =>
        createOrUpdateRNEAnnotation(s.name.value(), s)
      case s: NamedDomainElement with Linkable if s.linkLabel.option().isDefined =>
        createOrUpdateRNEAnnotation(s.linkLabel.value(), s)
      case _ => // ignore
    }
  }

  private def innerName(source: DomainElement with Linkable, resolved: DomainElement with NamedDomainElement): Unit =
    source match {
      case s: NamedDomainElement =>
        s.name.option() match {
          case Some(_) if hasAutoGeneratedName(s)                       => inner(source, resolved)
          case Some("schema" | "type" | "body") | None if source.isLink => inner(source, resolved)
          case Some(other) => resolved.withName(other, resolved.name.annotations())
          case _           =>
        }
      case _ =>
    }

  private def inner(source: DomainElement with Linkable, resolved: DomainElement with NamedDomainElement): Unit = {
    source.linkTarget match {
      case Some(target: Linkable) => innerName(target, resolved)
      case _                      => // ignore
    }
  }

  /** Check if it is an example. Special case where NamedExample fragments are used from an 'example' facet. */
  private def isExample(r: DomainElement) = r.meta.`type`.headOption.contains(Namespace.Document + "Example")

  private def isDeclaredElement(elem: DomainElement)    = elem.annotations.contains(classOf[DeclaredElement])
  private def hasAutoGeneratedName(elem: DomainElement) = elem.annotations.contains(classOf[AutoGeneratedName])

}

object ReferenceResolution {
  type Condition = (Linkable, DomainElement) => Boolean

  val VALID_DECLARATION_CONDITION: Condition = (link: Linkable, target: DomainElement) => {
    // TODO shouldn't we check that the target is a declared element instead of the link?
    link.annotations.contains(classOf[DeclaredElement]) && !target.isInstanceOf[ErrorDeclaration[_]]
  }

  val ASSERT_DIFFERENT: Condition = (link: Linkable, target: DomainElement) => link != target && link.id != target.id
}
