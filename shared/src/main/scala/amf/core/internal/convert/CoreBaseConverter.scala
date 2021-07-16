package amf.core.internal.convert

import amf.core.client.common.remote.Content
import amf.core.client.common.validation.{ProfileName, ValidationMode, ValidationProfile}
import amf.core.client.platform
import amf.core.client.platform.config.{
  AMFEventConverter,
  AMFEventListener => ClientAMFEventListener,
  ParsingOptions => ClientParsingOptions,
  RenderOptions => ClientRenderOptions
}
import amf.core.client.platform.errorhandling.ClientErrorHandler
import amf.core.client.platform.model.document.{
  BaseUnit => ClientBaseUnit,
  Document => ClientDocument,
  Module => ClientModule,
  PayloadFragment => ClientPayloadFragment
}
import amf.core.client.platform.model.domain.{
  AbstractDeclaration => ClientAbstractDeclaration,
  ArrayNode => ClientArrayNode,
  CustomDomainProperty => ClientCustomDomainProperty,
  DataNode => ClientDataNode,
  DomainElement => ClientDomainElement,
  DomainExtension => ClientDomainExtension,
  Graph => ClientGraph,
  ObjectNode => ClientObjectNode,
  ParametrizedDeclaration => ClientParameterizedDeclaration,
  PropertyShape => ClientPropertyShape,
  ScalarNode => ClientScalarNode,
  Shape => ClientShape,
  ShapeExtension => ClientShapeExtension,
  VariableValue => ClientVariableValue
}
import amf.core.client.platform.model.{
  Annotations => ClientAnnotations,
  AnyField => ClientAnyField,
  BoolField => ClientBoolField,
  DoubleField => ClientDoubleField,
  FloatField => ClientFloatField,
  IntField => ClientIntField,
  StrField => ClientStrField
}
import amf.core.client.platform.reference.{CachedReference => ClientCachedReference, UnitCache => ClientUnitCache}
import amf.core.client.platform.resource.{ResourceLoader => ClientResourceLoader}
import amf.core.client.platform.transform.{TransformationPipelineBuilder => ClientTransformationPipelineBuilder}
import amf.core.client.platform.validation.{
  payload,
  AMFValidationReport => ClientValidationReport,
  AMFValidationResult => ClientValidationResult,
  ValidationCandidate => ClientValidationCandidate,
  ValidationShapeSet => ClientValidationShapeSet
}
import amf.core.client.platform.{config, transform, AMFResult => ClientAMFResult}
import amf.core.client.scala.config._
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model._
import amf.core.client.scala.model.document.{BaseUnit, Document, Module, PayloadFragment}
import amf.core.client.scala.model.domain._
import amf.core.client.scala.model.domain.extensions.{
  CustomDomainProperty,
  DomainExtension,
  PropertyShape,
  ShapeExtension
}
import amf.core.client.scala.model.domain.templates.{AbstractDeclaration, ParametrizedDeclaration, VariableValue}
import amf.core.client.scala.resource.ResourceLoader
import amf.core.client.scala.transform.{TransformationPipelineBuilder, TransformationStep}
import amf.core.client.platform.validation.payload.{
  AMFShapePayloadValidator => ClientAMFShapePayloadValidator,
  ShapePayloadValidatorFactory => ClientShapePayloadValidatorFactory
}
import amf.core.client.scala.validation.payload.{
  AMFShapePayloadValidator,
  ShapePayloadValidatorFactory,
  ShapeValidationConfiguration,
  ValidatePayloadRequest
}
import amf.core.client.scala.validation.{AMFValidationReport, AMFValidationResult}
import amf.core.client.scala.{AMFGraphConfiguration, AMFResult}
import amf.core.internal.convert.PayloadValidatorConverter.PayloadValidatorMatcher
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.reference.UnitCacheAdapter
import amf.core.internal.remote.Vendor
import amf.core.internal.resource.{ClientResourceLoaderAdapter, InternalResourceLoaderAdapter}
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.internal.validation.{ValidationCandidate, ValidationShapeSet}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

trait CoreBaseConverter
    extends PlatformSecrets
    with CollectionConverter
    with FutureConverter
    with FieldConverter
    with CustomDomainPropertyConverter
    with ShapeConverter
    with PropertyShapeConverter
    with ShapeExtensionConverter
    with DataNodeConverter
    with DomainExtensionConverter
    with DocumentConverter
    with ModuleConverter
    with DeclarationsConverter
    with VariableValueConverter
    with ValidationConverter
    with DomainElementConverter
    with BaseUnitConverter
    with ResourceLoaderConverter
    with GraphDomainConverter
    with ValidationCandidateConverter
    with ValidationShapeSetConverter
    with PayloadFragmentConverter
    with CachedReferenceConverter
    with UnitCacheConverter
    with ParsingOptionsConverter
    with RenderOptionsConverter
    with AMFGraphConfigurationConverter
    with TransformationStepConverter
    with TransformationPipelineBuilderConverter
    with AMFResultConverter
    with AMFEventListenerConverter
    with ValidationProfileConverter
    with ShapeValidationConfigurationConverter
    with ValidatePayloadRequestConverter
    with ShapePayloadValidatorFactoryConverter {

  implicit def asClient[Internal, Client](from: Internal)(
      implicit m: InternalClientMatcher[Internal, Client]): Client =
    m.asClient(from)

  implicit def asInternal[Internal, Client](from: Client)(
      implicit m: ClientInternalMatcher[Client, Internal]): Internal = m.asInternal(from)

  implicit object StringMatcher      extends IdentityMatcher[String]
  implicit object BooleanMatcher     extends IdentityMatcher[Boolean]
  implicit object IntMatcher         extends IdentityMatcher[Int]
  implicit object LongMatcher        extends IdentityMatcher[Long]
  implicit object DoubleMatcher      extends IdentityMatcher[Double]
  implicit object FloatMatcher       extends IdentityMatcher[Float]
  implicit object AnyMatcher         extends IdentityMatcher[Any]
  implicit object UnitMatcher        extends IdentityMatcher[Unit]
  implicit object ProfileNameMatcher extends IdentityMatcher[ProfileName]
  implicit object VendorMatcher      extends IdentityMatcher[Vendor]

  implicit object ContentMatcher extends IdentityMatcher[Content]

  trait IdentityMatcher[T] extends InternalClientMatcher[T, T] with ClientInternalMatcher[T, T] {
    override def asClient(from: T): T   = from
    override def asInternal(from: T): T = from
  }
}

/** Return internal instance for given client representation. */
trait ClientInternalMatcher[Client, Internal] {
  def asInternal(from: Client): Internal
}

/** Return client instance for given internal representation. */
trait InternalClientMatcher[Internal, Client] {
  def asClient(from: Internal): Client
}

/** Matcher functioning in two directions. */
trait BidirectionalMatcher[Internal, Client]
    extends ClientInternalMatcher[Client, Internal]
    with InternalClientMatcher[Internal, Client]

/** Return internal instance for given client representation with implicit execution context. */
trait ClientInternalMatcherWithEC[Client, Internal] {
  def asInternal(from: Client)(implicit executionContext: ExecutionContext): Internal
}

/** Return client instance for given internal representation with implicit execution context. */
trait InternalClientMatcherWithEC[Internal, Client] {
  def asClient(from: Internal)(implicit executionContext: ExecutionContext): Client
}

/** Matcher functioning in two directions with implicit execution context. */
trait BidirectionalMatcherWithEC[Internal, Client]
    extends ClientInternalMatcherWithEC[Client, Internal]
    with InternalClientMatcherWithEC[Internal, Client]

trait FutureConverter {

  type ClientFuture[T]

  implicit class InternalFutureOps[Internal, Client](from: Future[Internal])(
      implicit m: InternalClientMatcher[Internal, Client],
      executionContext: ExecutionContext) {
    def asClient: ClientFuture[Client] = asClientFuture(from.map(m.asClient))
  }

  implicit class ClientFutureOps[Internal, Client](from: ClientFuture[Client])(
      implicit m: ClientInternalMatcher[Client, Internal],
      executionContext: ExecutionContext) {
    def asInternal: Future[Internal] = asInternalFuture(from, m)
  }

  protected def asClientFuture[T](from: Future[T])(implicit executionContext: ExecutionContext): ClientFuture[T]

  protected def asInternalFuture[Client, Internal](
      from: ClientFuture[Client],
      matcher: ClientInternalMatcher[Client, Internal])(implicit executionContext: ExecutionContext): Future[Internal]
}

trait CollectionConverter {

  type ClientOption[E]
  type ClientList[E]
  type ClientMap[V]

  trait ClientOptionLike[E] {
    def isEmpty: Boolean
    def folded[B](ifEmpty: => B)(f: E => B): B
    def mapped[B](f: E => B): ClientOption[B]
    def getOrNull: E
    def getOrElse[B >: E](default: => B): B
  }

  implicit class InternalOptionOps[Internal, Client](from: Option[Internal])(
      implicit m: InternalClientMatcher[Internal, Client]) {
    def asClient: ClientOption[Client] = asClientOption(from, m)
  }

  implicit class InternalOptionOpsWithEC[Internal, Client](from: Option[Internal])(
      implicit m: InternalClientMatcherWithEC[Internal, Client],
      executionContext: ExecutionContext) {
    def asClient: ClientOption[Client] = asClientOptionWithEC(from, m)
  }

  implicit class ClientListOps[Internal, Client](from: ClientList[Client])(
      implicit m: ClientInternalMatcher[Client, Internal]) {
    def asInternal: Seq[Internal] = asInternalSeq(from, m)
  }

  implicit class ClientListOpsWithEC[Internal, Client](from: ClientList[Client])(
      implicit m: ClientInternalMatcherWithEC[Client, Internal],
      executionContext: ExecutionContext) {
    def asInternal: Seq[Internal] = asInternalSeqWithEC(from, m)
  }

  implicit class ClientOptionOps[Client](from: ClientOption[Client]) {
    def toScala: Option[Client] = toScalaOption(from)
  }

  implicit class ClientMapOps[Internal, Client](from: ClientMap[Client])(
      implicit m: ClientInternalMatcher[Client, Internal]
  ) {
    def asInternal: Map[String, Internal] = asInternalMap(from, m)
  }

  //  implicit class ClientOptionOpsWithEC[Client](from: ClientOption[Client])(
//      implicit executionContext: ExecutionContext) {
//    def toScala: Option[Client] = toScalaOptionWithEC(from)
//  }

  implicit class InternalMapOps[Internal, Client](from: mutable.Map[String, Internal])(
      implicit m: InternalClientMatcher[Internal, Client]) {
    def asClient: ClientMap[Client] = asClientMap(from, m)
  }

  implicit class InternalImmutableMapOps[Internal, Client](from: Map[String, Internal])(
      implicit m: InternalClientMatcher[Internal, Client]) {
    def asClient: ClientMap[Client] = asClientImmutableMap(from, m)
  }

  implicit class InternalLinkedMapOps[Internal, Client](from: mutable.LinkedHashMap[String, Internal])(
      implicit m: InternalClientMatcher[Internal, Client]) {
    def asClient: ClientMap[Client] = asClientLinkedMap(from, m)
  }

  implicit class InternalSeqOps[Internal, Client](from: Seq[Internal])(
      implicit m: InternalClientMatcher[Internal, Client]) {
    def asClient: ClientList[Client] = asClientList(from, m)
  }

  implicit class InternalSeqOpsWithEC[Internal, Client](from: Seq[Internal])(
      implicit m: InternalClientMatcherWithEC[Internal, Client],
      executionContext: ExecutionContext) {
    def asClient: ClientList[Client] = asClientListWithEC(from, m)
  }

  protected def asClientOption[Internal, Client](from: Option[Internal],
                                                 m: InternalClientMatcher[Internal, Client]): ClientOption[Client]

  protected def asClientOptionWithEC[Internal, Client](from: Option[Internal],
                                                       m: InternalClientMatcherWithEC[Internal, Client])(
      implicit executionContext: ExecutionContext): ClientOption[Client]

  protected def toScalaOption[E](from: ClientOption[E]): Option[E]

  protected def toClientOption[E](from: Option[E]): ClientOption[E]

//  protected def toScalaOptionWithEC[E](from: ClientOption[E])(implicit executionContext: ExecutionContext): Option[E]
//
//  protected def toClientOptionWithEC[E](from: Option[E])(implicit executionContext: ExecutionContext): ClientOption[E]

  private[convert] def asClientList[Internal, Client](from: Seq[Internal],
                                                      m: InternalClientMatcher[Internal, Client]): ClientList[Client]

  private[convert] def asClientListWithEC[Internal, Client](from: Seq[Internal],
                                                            m: InternalClientMatcherWithEC[Internal, Client])(
      implicit executionContext: ExecutionContext): ClientList[Client]

  protected def asClientMap[Internal, Client](from: mutable.Map[String, Internal],
                                              m: InternalClientMatcher[Internal, Client]): ClientMap[Client]

  protected def asClientImmutableMap[Internal, Client](from: Map[String, Internal],
                                                       m: InternalClientMatcher[Internal, Client]): ClientMap[Client]

  protected def asClientLinkedMap[Internal, Client](from: mutable.LinkedHashMap[String, Internal],
                                                    m: InternalClientMatcher[Internal, Client]): ClientMap[Client]

  protected def asInternalSeq[Client, Internal](from: ClientList[Client],
                                                m: ClientInternalMatcher[Client, Internal]): Seq[Internal]

  protected def asInternalSeqWithEC[Client, Internal](
      from: ClientList[Client],
      m: ClientInternalMatcherWithEC[Client, Internal])(implicit executionContext: ExecutionContext): Seq[Internal]

  protected def asInternalMap[Client, Internal](from: ClientMap[Client],
                                                m: ClientInternalMatcher[Client, Internal]): Map[String, Internal]

}

trait FieldConverter extends CollectionConverter {

  implicit object StrFieldMatcher extends InternalClientMatcher[StrField, ClientStrField] {
    override def asClient(from: StrField): ClientStrField = ClientStrField(from)
  }

  implicit object IntFieldMatcher extends InternalClientMatcher[IntField, ClientIntField] {
    override def asClient(from: IntField): ClientIntField = ClientIntField(from)
  }

  implicit object BoolFieldMatcher extends InternalClientMatcher[BoolField, ClientBoolField] {
    override def asClient(from: BoolField): ClientBoolField = ClientBoolField(from)
  }

  implicit object DoubleFieldMatcher extends InternalClientMatcher[DoubleField, ClientDoubleField] {
    override def asClient(from: DoubleField): ClientDoubleField = ClientDoubleField(from)
  }

  implicit object FloatFieldMatcher extends InternalClientMatcher[FloatField, ClientFloatField] {
    override def asClient(from: FloatField): ClientFloatField = ClientFloatField(from)
  }

  implicit object AnyFieldMatcher extends InternalClientMatcher[AnyField, ClientAnyField] {
    override def asClient(from: AnyField): ClientAnyField = ClientAnyField(from)
  }

  implicit object AnnotationsFieldMatcher extends BidirectionalMatcher[Annotations, ClientAnnotations] {
    override def asClient(from: Annotations): ClientAnnotations = ClientAnnotations(from)

    override def asInternal(from: ClientAnnotations): Annotations = from._internal
  }
}

trait DomainExtensionConverter {

  implicit object DomainExtensionMatcher extends BidirectionalMatcher[DomainExtension, ClientDomainExtension] {
    override def asClient(from: DomainExtension): ClientDomainExtension = ClientDomainExtension(from)

    override def asInternal(from: ClientDomainExtension): DomainExtension = from._internal
  }
}

trait DataNodeConverter {

  implicit object ObjectNodeMatcher extends BidirectionalMatcher[ObjectNode, ClientObjectNode] {
    override def asClient(from: ObjectNode): ClientObjectNode   = ClientObjectNode(from)
    override def asInternal(from: ClientObjectNode): ObjectNode = from._internal
  }

  implicit object ScalarNodeMatcher extends BidirectionalMatcher[ScalarNode, ClientScalarNode] {
    override def asClient(from: ScalarNode): ClientScalarNode   = ClientScalarNode(from)
    override def asInternal(from: ClientScalarNode): ScalarNode = from._internal
  }

  implicit object ArrayNodeMatcher extends BidirectionalMatcher[ArrayNode, ClientArrayNode] {
    override def asClient(from: ArrayNode): ClientArrayNode   = ClientArrayNode(from)
    override def asInternal(from: ClientArrayNode): ArrayNode = from._internal
  }

  implicit object DataNodeMatcher extends BidirectionalMatcher[DataNode, ClientDataNode] {
    override def asClient(from: DataNode): ClientDataNode = from match {
      case o: ObjectNode => ObjectNodeMatcher.asClient(o)
      case s: ScalarNode => ScalarNodeMatcher.asClient(s)
      case a: ArrayNode  => ArrayNodeMatcher.asClient(a)
      case _ => // noinspection ScalaStyle
        null
    }
    override def asInternal(from: ClientDataNode): DataNode = from._internal
  }
}

trait DeclarationsConverter extends PlatformSecrets {

  implicit object AbstractDeclarationMatcher
      extends BidirectionalMatcher[AbstractDeclaration, ClientAbstractDeclaration] {
    override def asClient(from: AbstractDeclaration): ClientAbstractDeclaration =
      platform.wrap[ClientAbstractDeclaration](from)

    override def asInternal(from: ClientAbstractDeclaration): AbstractDeclaration = from._internal
  }

  implicit object ParameterizedDeclarationMatcher
      extends BidirectionalMatcher[ParametrizedDeclaration, ClientParameterizedDeclaration] {
    override def asClient(from: ParametrizedDeclaration): ClientParameterizedDeclaration =
      platform.wrap[ClientParameterizedDeclaration](from)

    override def asInternal(from: ClientParameterizedDeclaration): ParametrizedDeclaration = from._internal
  }

}

trait ShapeConverter extends PlatformSecrets {

  implicit object ShapeMatcher extends BidirectionalMatcher[Shape, ClientShape] {
    override def asClient(from: Shape): ClientShape = platform.wrap[ClientShape](from)

    override def asInternal(from: ClientShape): Shape = from._internal
  }

}

trait PropertyShapeConverter extends PlatformSecrets {

  implicit object PropertyShapeMatcher extends BidirectionalMatcher[PropertyShape, ClientPropertyShape] {
    override def asClient(from: PropertyShape): ClientPropertyShape = platform.wrap[ClientPropertyShape](from)

    override def asInternal(from: ClientPropertyShape): PropertyShape = from._internal
  }

}

trait ShapeExtensionConverter extends PlatformSecrets {

  implicit object ShapeExtensionMatcher extends BidirectionalMatcher[ShapeExtension, ClientShapeExtension] {
    override def asClient(from: ShapeExtension): ClientShapeExtension = ClientShapeExtension(from)

    override def asInternal(from: ClientShapeExtension): ShapeExtension = from._internal
  }

}

trait DocumentConverter extends PlatformSecrets {
  implicit object DocumentMatcher extends BidirectionalMatcher[Document, ClientDocument] {
    override def asClient(from: Document): ClientDocument = new ClientDocument(from)

    override def asInternal(from: ClientDocument): Document = from._internal
  }
}

trait ModuleConverter extends PlatformSecrets {
  implicit object ModuleMatcher extends BidirectionalMatcher[Module, ClientModule] {
    override def asClient(from: Module): ClientModule = ClientModule(from)

    override def asInternal(from: ClientModule): Module = from._internal
  }
}

trait VariableValueConverter {

  implicit object VariableValueMatcher extends BidirectionalMatcher[VariableValue, ClientVariableValue] {
    override def asInternal(from: ClientVariableValue): VariableValue = from._internal

    override def asClient(from: VariableValue): ClientVariableValue = ClientVariableValue(from)
  }

}

trait ValidationConverter {
  implicit object ValidationReportMatcher extends BidirectionalMatcher[AMFValidationReport, ClientValidationReport] {
    override def asClient(from: AMFValidationReport): ClientValidationReport = new ClientValidationReport(from)

    override def asInternal(from: ClientValidationReport): AMFValidationReport = from._internal
  }

  implicit object ValidationResultMatcher extends BidirectionalMatcher[AMFValidationResult, ClientValidationResult] {
    override def asClient(from: AMFValidationResult): ClientValidationResult = new ClientValidationResult(from)

    override def asInternal(from: ClientValidationResult): AMFValidationResult = from._internal
  }
}

trait DomainElementConverter extends PlatformSecrets {

  implicit object DomainElementMatcher extends BidirectionalMatcher[DomainElement, ClientDomainElement] {
    override def asInternal(from: ClientDomainElement): DomainElement = from._internal

    override def asClient(from: DomainElement): ClientDomainElement = platform.wrap[ClientDomainElement](from)
  }

}

trait BaseUnitConverter extends PlatformSecrets {

  implicit object BaseUnitMatcher extends BidirectionalMatcher[BaseUnit, ClientBaseUnit] {
    override def asInternal(from: ClientBaseUnit): BaseUnit = from._internal

    override def asClient(from: BaseUnit): ClientBaseUnit = platform.wrap[ClientBaseUnit](from)
  }

}

trait PayloadFragmentConverter extends PlatformSecrets {

  implicit object PayloadFragmentMatcher extends BidirectionalMatcher[PayloadFragment, ClientPayloadFragment] {
    override def asInternal(from: ClientPayloadFragment): PayloadFragment = from._internal

    override def asClient(from: PayloadFragment): ClientPayloadFragment = platform.wrap[ClientPayloadFragment](from)
  }

}

trait CustomDomainPropertyConverter {

  implicit object CustomDomainPropertyMatcher
      extends BidirectionalMatcher[CustomDomainProperty, ClientCustomDomainProperty] {
    override def asInternal(from: ClientCustomDomainProperty): CustomDomainProperty = from._internal

    override def asClient(from: CustomDomainProperty): ClientCustomDomainProperty = ClientCustomDomainProperty(from)
  }

}

trait GraphDomainConverter {

  implicit object GraphDomainConverter extends BidirectionalMatcher[Graph, ClientGraph] {
    override def asInternal(from: ClientGraph): Graph = from._internal

    override def asClient(from: Graph): ClientGraph = ClientGraph(from)
  }

}

trait ResourceLoaderConverter {
  type ClientLoader <: ClientResourceLoader
  type Loader

  implicit object ResourceLoaderMatcher extends BidirectionalMatcherWithEC[ResourceLoader, ClientResourceLoader] {
    override def asInternal(from: ClientResourceLoader)(implicit executionContext: ExecutionContext): ResourceLoader =
      InternalResourceLoaderAdapter(from)

    override def asClient(from: ResourceLoader)(implicit executionContext: ExecutionContext): ClientResourceLoader =
      from match {
        case InternalResourceLoaderAdapter(adaptee) => adaptee
        case _                                      => ClientResourceLoaderAdapter(from)
      }
  }

}

trait UnitCacheConverter {
  type ClientReference <: ClientUnitCache

  implicit object UnitCacheMatcher extends BidirectionalMatcherWithEC[UnitCache, ClientUnitCache] {
    override def asInternal(from: ClientUnitCache)(implicit executionContext: ExecutionContext): UnitCache =
      UnitCacheAdapter(from)

    override def asClient(from: UnitCache)(implicit executionContext: ExecutionContext): ClientUnitCache =
      from match {
        case UnitCacheAdapter(adaptee) => adaptee
      }
  }

}

trait CachedReferenceConverter extends PlatformSecrets {

  implicit object CachedReferenceMatcher extends BidirectionalMatcher[CachedReference, ClientCachedReference] {
    override def asInternal(from: ClientCachedReference): CachedReference = from._internal

    override def asClient(from: CachedReference): ClientCachedReference = ClientCachedReference(from)
  }

}

trait ValidationCandidateConverter {

  implicit object ValidationCandidateMatcher
      extends BidirectionalMatcher[ValidationCandidate, ClientValidationCandidate] {
    override def asClient(from: ValidationCandidate): ClientValidationCandidate = ClientValidationCandidate(from)

    override def asInternal(from: ClientValidationCandidate): ValidationCandidate = from._internal
  }
}

trait ValidationShapeSetConverter {

  implicit object ValidationShapeSetMatcher
      extends BidirectionalMatcher[ValidationShapeSet, ClientValidationShapeSet] {
    override def asClient(from: ValidationShapeSet): ClientValidationShapeSet = ClientValidationShapeSet(from)

    override def asInternal(from: ClientValidationShapeSet): ValidationShapeSet = from._internal
  }
}

trait ParsingOptionsConverter {
  implicit object ParsingOptionsMatcher extends BidirectionalMatcher[ParsingOptions, config.ParsingOptions] {
    override def asClient(from: ParsingOptions): config.ParsingOptions   = ClientParsingOptions(from)
    override def asInternal(from: config.ParsingOptions): ParsingOptions = from._internal
  }
}

trait RenderOptionsConverter {
  implicit object RenderOptionsMatcher extends BidirectionalMatcher[RenderOptions, config.RenderOptions] {
    override def asClient(from: RenderOptions): config.RenderOptions   = ClientRenderOptions(from)
    override def asInternal(from: config.RenderOptions): RenderOptions = from._internal
  }
}

trait AMFGraphConfigurationConverter {
  implicit object AMFGraphConfigurationMatcher
      extends BidirectionalMatcher[AMFGraphConfiguration, platform.AMFGraphConfiguration] {
    override def asClient(from: AMFGraphConfiguration): platform.AMFGraphConfiguration =
      new platform.AMFGraphConfiguration(from)
    override def asInternal(from: platform.AMFGraphConfiguration): AMFGraphConfiguration = from._internal
  }
}

trait TransformationStepConverter extends BaseUnitConverter {
  implicit object TransformationStepMatcher
      extends BidirectionalMatcher[TransformationStep, transform.TransformationStep] {
    override def asClient(from: TransformationStep): transform.TransformationStep = {
      (model: ClientBaseUnit, errorHandler: ClientErrorHandler) =>
        {
          val result: BaseUnit =
            from.transform(BaseUnitMatcher.asInternal(model), ClientErrorHandlerConverter.convert(errorHandler))
          BaseUnitMatcher.asClient(result)
        }
    }
    override def asInternal(from: transform.TransformationStep): TransformationStep = {
      (model: BaseUnit, errorHandler: AMFErrorHandler) =>
        {
          val result: ClientBaseUnit =
            from.transform(BaseUnitMatcher.asClient(model), ClientErrorHandlerConverter.convertToClient(errorHandler))
          BaseUnitMatcher.asInternal(result)
        }
    }
  }
}

trait TransformationPipelineBuilderConverter {
  implicit object TransformationPipelineBuilderMatcher
      extends BidirectionalMatcher[TransformationPipelineBuilder, transform.TransformationPipelineBuilder] {
    override def asClient(from: TransformationPipelineBuilder): transform.TransformationPipelineBuilder =
      ClientTransformationPipelineBuilder(from)
    override def asInternal(from: transform.TransformationPipelineBuilder): TransformationPipelineBuilder =
      from._internal
  }
}

trait AMFResultConverter {
  implicit object AMFResultMatcher extends BidirectionalMatcher[AMFResult, platform.AMFResult] {
    override def asClient(from: AMFResult): platform.AMFResult =
      ClientAMFResult(from)
    override def asInternal(from: platform.AMFResult): AMFResult = from._internal
  }
}

trait AMFEventListenerConverter {
  implicit object AMFEventListenerMatcher extends ClientInternalMatcher[ClientAMFEventListener, AMFEventListener] {
    override def asInternal(from: ClientAMFEventListener): AMFEventListener = { (event: AMFEvent) =>
      {
        val clientEvent = AMFEventConverter.asClient(event)
        from.notifyEvent(clientEvent)
      }
    }
  }
}

trait ValidationProfileConverter {
  implicit object ValidationProfileMatcher
      extends BidirectionalMatcher[amf.core.internal.validation.core.ValidationProfile, ValidationProfile] {
    override def asClient(from: amf.core.internal.validation.core.ValidationProfile): ValidationProfile =
      new ValidationProfile(from)

    override def asInternal(from: ValidationProfile): amf.core.internal.validation.core.ValidationProfile =
      from.internal
  }
}

trait ShapeValidationConfigurationConverter {
  implicit object ShapeValidationConfigurationMatcher
      extends BidirectionalMatcher[ShapeValidationConfiguration, payload.ShapeValidationConfiguration] {

    override def asInternal(from: payload.ShapeValidationConfiguration): ShapeValidationConfiguration = from._internal
    override def asClient(from: ShapeValidationConfiguration): payload.ShapeValidationConfiguration =
      new payload.ShapeValidationConfiguration(from)
  }
}

trait ValidatePayloadRequestConverter {
  implicit object ValidatePayloadRequestMatcher
      extends BidirectionalMatcher[ValidatePayloadRequest, payload.ValidatePayloadRequest] {

    override def asInternal(from: payload.ValidatePayloadRequest): ValidatePayloadRequest = from._internal
    override def asClient(from: ValidatePayloadRequest): payload.ValidatePayloadRequest =
      payload.ValidatePayloadRequest(from)
  }
}

trait ShapePayloadValidatorFactoryConverter {
  implicit object ShapePayloadValidatorFactoryMatcher
      extends InternalClientMatcherWithEC[ShapePayloadValidatorFactory, ClientShapePayloadValidatorFactory] {
    override def asClient(from: ShapePayloadValidatorFactory)(
        implicit executionContext: ExecutionContext): ClientShapePayloadValidatorFactory = {
      new ClientShapePayloadValidatorFactory {
        override def createFor(shape: ClientShape,
                               mediaType: String,
                               mode: ValidationMode): ClientAMFShapePayloadValidator =
          PayloadValidatorMatcher.asClient(from.createFor(shape._internal, mediaType, mode))
        override def createFor(shape: ClientShape, fragment: ClientPayloadFragment): ClientAMFShapePayloadValidator =
          PayloadValidatorMatcher.asClient(from.createFor(shape._internal, fragment._internal))
      }
    }
  }
}
