package amf.core.client.scala.model.domain

import amf.core.client.scala.model.{BoolField, StrField}
import amf.core.client.scala.parse.document.UnresolvedComponents
import amf.core.internal.metamodel.domain.LinkableElementModel
import amf.core.internal.parser.domain.{Annotations, DeclarationPromise, Fields, ScalarNode => ScalarNodeObj}
import amf.core.internal.utils.IdCounter
import amf.core.internal.adoption.AdoptionDependantCalls
import amf.core.internal.validation.CoreValidations.{UnresolvedReference, UnresolvedReferenceWarning}
import org.mulesoft.common.client.lexical.SourceLocation

trait Linkable extends AmfObject with AdoptionDependantCalls { this: DomainElement with Linkable =>

  def linkTarget: Option[DomainElement]    = Option(fields(LinkableElementModel.Target))
  var linkAnnotations: Option[Annotations] = None
  def supportsRecursion: BoolField         = fields.field(LinkableElementModel.SupportsRecursion)
  def effectiveLinkTarget(links: Set[Linkable] = Set()): DomainElement =
    linkTarget
      .map {
        case linkable: Linkable if linkTarget.isDefined =>
          if (links.contains(linkable)) linkable.linkTarget.get
          else linkable.effectiveLinkTarget(links + linkable)
        case other => other
      }
      .getOrElse(this)

  def isLink: Boolean     = linkTarget.isDefined
  def linkLabel: StrField = fields.field(LinkableElementModel.Label)

  def linkCopy(): Linkable

  def withLinkTarget(target: DomainElement): this.type = {
    fields.setWithoutId(LinkableElementModel.Target, target, Annotations.synthesized())
    set(LinkableElementModel.TargetId, AmfScalar(target.id), Annotations.synthesized())
  }

  callAfterAdoption { () =>
    linkTarget
      .map(_.id)
      .foreach(targetId => set(LinkableElementModel.TargetId, AmfScalar(targetId), Annotations.synthesized()))
  }

  def withLinkLabel(label: String, annotations: Annotations = Annotations()): this.type =
    set(LinkableElementModel.Label, AmfScalar(label, annotations), Annotations.inferred())
  def withSupportsRecursion(recursive: Boolean): this.type =
    set(LinkableElementModel.SupportsRecursion, AmfScalar(recursive), Annotations.synthesized())

  def link[T](label: String, annotations: Annotations = Annotations()): T = {
    link(AmfScalar(label), annotations, Annotations())
  }

  private[amf] def link[T](label: ScalarNodeObj, annotations: Annotations): T = {
    link(label.text(), annotations, Annotations.inferred())
  }

  private[amf] def link[T](label: AmfScalar, annotations: Annotations, fieldAnn: Annotations): T = {
    val copied = linkCopy()
    val hash = buildLinkHash(
        Option(label.value).map(_.toString).getOrElse(""),
        annotations
    ) // todo: label.value is sometimes null!
    copied
      .withId(s"${copied.id}/link-$hash")
      .withLinkTarget(this)
      .set(LinkableElementModel.Label, label, fieldAnn)
      .add(annotations)
      .asInstanceOf[T]
  }

  private def buildLinkHash(label: String, annotations: Annotations): Int = {
    val sb = new StringBuilder
    sb.append(label)
    annotations.foreach {
      case s: SerializableAnnotation =>
        sb.append(s.name)
        sb.append(s.value)
      case _ => // Ignore
    }
    sb.toString().hashCode
  }

  /** This can be overriden by subclasses to customise how the links to unresolved classes are generated. By default it
    * just generates a link.
    */
  private[amf] def resolveUnreferencedLink[T](
      label: String,
      annotations: Annotations = Annotations(),
      unresolved: T,
      supportsRecursion: Boolean
  ): T = {
    if (unresolved.asInstanceOf[Linkable].shouldLink) {

      val linked: T = link(AmfScalar(label), annotations, Annotations.synthesized())
      if (supportsRecursion && linked.isInstanceOf[Linkable])
        linked.asInstanceOf[Linkable].withSupportsRecursion(supportsRecursion)
      linked
    } else
      this.asInstanceOf[T]
  }

  protected val shouldLink: Boolean = true

  private[amf] def afterResolve(fatherSyntaxKey: Option[String], resolvedId: String): Unit = Unit

  // Unresolved references to things that can be linked
  // TODO: another trait?
  private[amf] var isUnresolved: Boolean           = false
  private[amf] var refName                         = ""
  private[amf] var refAliases                      = Seq[String]()
  private var unresolvedSeverity: String           = "error"
  private var astPos: Option[SourceLocation]       = None
  private var refCtx: Option[UnresolvedComponents] = None

  def unresolved(
      refName: String,
      aliases: Seq[String],
      pos: Option[SourceLocation],
      unresolvedSeverity: String = "error"
  )(implicit ctx: UnresolvedComponents): DomainElement with Linkable = {
    isUnresolved = true
    this.unresolvedSeverity = unresolvedSeverity
    this.refName = refName
    this.refAliases = aliases
    this.astPos = pos
    refCtx = Some(ctx)
    this
  }

  private[amf] def toFutureRef(resolve: Linkable => Unit): Unit = {
    refCtx match {
      case Some(ctx) =>
        val promise = DeclarationPromise(
            resolve,
            () =>
              if (unresolvedSeverity == "warning") {
                ctx.eh.warning(UnresolvedReferenceWarning, this, s"Unresolved reference '$refName'", astPos.get)

              } else
                ctx.eh.violation(UnresolvedReference, this, s"Unresolved reference '$refName'", astPos.get)
        )
        (Seq(refName) ++ refAliases).foreach { ref =>
          ctx.futureDeclarations.futureRef(
              id,
              ref,
              promise
          )
        }
      case _ => throw new Exception("Cannot create unresolved reference with missing parsing context")
    }
  }

  private val linkCounter = new IdCounter()

  /** generates a new instance of the domain element only clonning his own fields map, and not clonning all the tree
    * (not recursive)
    */
  /** Do not generates a new link. */
  def copyElement(): Linkable with DomainElement = classConstructor(fields.copy(), annotations.copy())

  def copyElement(a: Annotations): Linkable with DomainElement = classConstructor(fields.copy(), a)

  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement
}
