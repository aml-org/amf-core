package amf.core.resolution.stages.elements.resolution
import amf.core.annotations._
import amf.core.errorhandling.AMFErrorHandler
import amf.core.metamodel.document.DocumentModel
import amf.core.model.document.Document
import amf.core.model.domain.{AmfObject, DomainElement, LinkNode, Linkable, NamedDomainElement}
import amf.core.parser.Annotations
import amf.core.resolution.stages.elements.resolution.ReferenceResolution.{Condition, VALID_DECLARATION_CONDITION}
import amf.core.resolution.stages.helpers.{LinkNodeResolver, ModelReferenceResolver, ResolvedNamedEntity}
import amf.core.resolution.stages.LinkNodeResolutionStage
import amf.core.resolution.stages.selectors.{LinkNodeSelector, LinkSelector}
import amf.core.traversal.{
  DomainElementSelectorAdapter,
  DomainElementTransformationAdapter,
  TransformationData,
  TransformationTraversal
}
import amf.core.vocabulary.Namespace

import scala.collection.mutable

class ReferenceResolution(errorHandler: AMFErrorHandler,
                          keepEditingInfo: Boolean = false,
                          modelResolver: Option[ModelReferenceResolver] = None,
                          cache: mutable.Map[String, DomainElement] = mutable.Map(),
                          customDomainElementTransformation: (DomainElement, Linkable) => DomainElement =
                            (d: DomainElement, _: Linkable) => d)
    extends ElementStageTransformer[DomainElement] {

  override def transform(element: DomainElement): Option[DomainElement] =
    transform(element, Seq(VALID_DECLARATION_CONDITION))

  def transform(element: DomainElement, conditions: Seq[Condition]): Option[DomainElement] = {
    element match {
      case l: Linkable if l.isLink =>
        if (cache.contains(l.linkTarget.get.id)) Some(cache(l.linkTarget.get.id))
        else if (cache.contains(element.id) && shouldCopyElement(conditions, l, cache(element.id))) {
          val cached = cache(element.id)
          Some(copyEffectiveLinkTarget(element, cached.asInstanceOf[DomainElement with Linkable]))
        } else {
          val target   = resolveLinkTarget(element, conditions, l)
          var resolved = innerLinkNodeResolution(target)
          resolved match {
            case linkable: Linkable if l.supportsRecursion.option().getOrElse(false) =>
              linkable.withSupportsRecursion(true)
            case _ => // ignore
          }
          resolved = customDomainElementTransformation(withName(resolved, l), l)
          resolved.annotations += ResolvedInheritance()
          if (keepEditingInfo) addResolvedLinkAnnotations(l, resolved)
          addIntermediateLinkTargetsToCache(element, resolved)
          resolved = traverseNestedLinksIfCopy(element, conditions, l, resolved)
          Some(resolved)
        }
      case ln: LinkNode => LinkNodeResolver.resolveDynamicLink(ln, modelResolver, keepEditingInfo)
      case _            => None
    }
  }

  private def traverseNestedLinksIfCopy(element: DomainElement,
                                        conditions: Seq[Condition],
                                        l: DomainElement with Linkable,
                                        resolved: DomainElement) = {
    l.effectiveLinkTarget() match {
      case t: DomainElement with Linkable if shouldCopyElement(conditions, l, t) =>
        cache.put(element.id, resolved)
        resolveNestedLinks(resolved)
      case _ => resolved
    }
  }

  private def shouldCopyElement(conditions: Seq[Condition],
                                linkable: Linkable,
                                linkableAsElement: DomainElement): Boolean = {
    conditions.forall(condition => condition(linkable, linkableAsElement))
  }

  private def resolveNestedLinks(domainElement: DomainElement): DomainElement = {
    val selectorAdapter       = new DomainElementSelectorAdapter(LinkSelector || LinkNodeSelector)
    val transformationAdapter = new DomainElementTransformationAdapter((elem, _) => transform(elem))
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
    link.annotations.find(classOf[TrackedElement]).foreach {
      case annotation @ TrackedElement(values) => addLinkTrackedElementsToResolvedElement(element, annotation, values)
    }
    if (hasAutoGeneratedName(link) && !isDeclaredElement(element)) element.add(AutoGeneratedName())
  }

  private def addLinkTrackedElementsToResolvedElement(element: DomainElement, t: TrackedElement, values: Set[String]) = {
    val tracked = element.annotations
      .find(classOf[TrackedElement])
      .fold(t)(inner => TrackedElement(values ++ inner.parents))

    element.annotations.reject(_.isInstanceOf[TrackedElement])
    element.add(tracked)
  }

  // Links traversion to expand annotations and add links to 'cache'
  @scala.annotation.tailrec
  private def addIntermediateLinkTargetsToCache(element: DomainElement,
                                                resolved: DomainElement,
                                                visited: mutable.Set[String] = mutable.Set()): Unit = {
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

  private def innerLinkNodeResolution(target: DomainElement): DomainElement = {
    val nested = Document()
    nested.fields.setWithoutId(DocumentModel.Encodes, target)
    val result = new LinkNodeResolutionStage(keepEditingInfo).transform(nested, errorHandler)
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
        } else if (r.name.option().isEmpty || r.name.value() == "schema" || r.name.value() == "type" || r.name
                     .value() == "body" || r.annotations.contains(classOf[AutoGeneratedName])) {
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
          case Some(other)                                              => resolved.withName(other, resolved.name.annotations())
          case _                                                        =>
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
