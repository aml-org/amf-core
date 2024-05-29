package amf.core.client.scala.model.document

import amf.core.client.common.validation.ProfileName
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.FieldsFilter.Local
import amf.core.client.scala.model.domain._
import amf.core.client.scala.model.{BoolField, StrField}
import amf.core.client.scala.traversal.iterator._
import amf.core.client.scala.traversal.{
  DomainElementSelectorAdapter,
  DomainElementTransformationAdapter,
  TransformationData,
  TransformationTraversal
}
import amf.core.internal.annotations.{Aliases, ReferencedInfo}
import amf.core.internal.metamodel.MetaModelTypeMapping
import amf.core.internal.metamodel.document.BaseUnitModel
import amf.core.internal.metamodel.document.BaseUnitModel._
import amf.core.internal.metamodel.document.DocumentModel.References
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.remote.Spec
import amf.core.internal.unsafe.PlatformSecrets

import scala.collection.mutable

/** Any parseable unit, backed by a source URI. */
trait BaseUnit extends AmfObject with MetaModelTypeMapping with PlatformSecrets {

  // Set the default parsingRoot
  withRoot(false)

  // Set default processing data
  setDefaultProcessingData()

  private[amf] def setDefaultProcessingData(): Unit =
    withProcessingData {
      BaseUnitProcessingData().withTransformed(false)
    }

  @deprecated("Use processingData.transformed instead", "AMF 5.0.0 & AML 6.0.0")
  protected[amf] var resolved: Boolean = false

  /** Raw text  used to generated this unit */
  var raw: Option[String] = None

  /** Meta data for the document */
  def meta: BaseUnitModel

  /** Returns the list document URIs referenced from the document that has been parsed to generate this model */
  def references: Seq[BaseUnit]

  /** Returns package */
  def pkg: StrField = fields.field(Package)

  /** Returns the file location for the document that has been parsed to generate this model */
  override def location(): Option[String] = {
    val fieldValue: StrField = fields.field(Location)
    fieldValue.option().orElse(super.location())
  }

  /** Returns the usage. */
  def usage: StrField = fields.field(Usage)

  /** Returns true if the base unit is the root of the document model obtained from parsing * */
  def root: BoolField = fields.field(Root)

  /** Returns the version. */
  @deprecated("Use processingData.modelVersion for API Contract Base Units only", "AMF 5.0.0 & AML 6.0.0")
  def modelVersion: StrField = fields.field(ModelVersion)

  /** Set the raw value for the base unit */
  def withRaw(raw: String): BaseUnit = {
    this.raw = Some(raw)
    this
  }

  /** Set references
    * @param references
    *   references to set
    * @return
    *   Previous [[BaseUnit]] with references set
    */
  def withReferences(references: Seq[BaseUnit]): this.type = setArrayWithoutId(References, references)

  /** Set location
    * @param location
    *   location to set
    * @return
    *   Previous [[BaseUnit]] with location set
    */
  def withLocation(location: String): this.type = set(Location, location)

  /** Set usage
    * @param usage
    *   usage to set
    * @return
    *   Previous [[BaseUnit]] with usage set
    */
  def withUsage(usage: String): this.type = set(Usage, usage)

  def processingData: BaseUnitProcessingData = fields.field(ProcessingData)

  def withProcessingData(data: BaseUnitProcessingData): this.type = set(ProcessingData, data)

  def sourceInformation: BaseUnitSourceInformation = fields.field(SourceInformation)

  @deprecated("Use processingData.withModelVersion for API Contract Base Units only", "AMF 5.0.0 & AML 6.0.0")
  private def withModelVersion(version: String): this.type = set(ModelVersion, version)

  def withRoot(value: Boolean): this.type = set(Root, value)

  def addReference(newRef: BaseUnit): Unit = synchronized(withReferences(references :+ newRef))

  def withPkg(pkg: String): this.type = set(Package, pkg)
  def withPkg(pkg: String, annotations: Annotations): this.type =
    set(Package, AmfScalar(pkg, annotations), Annotations.inferred())

  private[amf] def withSourceInformation(sourceInfo: BaseUnitSourceInformation): this.type =
    set(SourceInformation, sourceInfo)

  /** Returns Unit iterator for specified strategy and scope. */
  def iterator(
      strategy: IteratorStrategy = DomainElementStrategy,
      fieldsFilter: FieldsFilter = Local,
      visited: VisitedCollector = IdCollector()
  ): AmfIterator = {
    strategy.iterator(fieldsFilter.filter(fields), visited)
  }

  /** Finds first domain element with the requested id
    */
  def findById(id: String): Option[DomainElement] =
    iterator(fieldsFilter = FieldsFilter.All).collectFirst {
      case e: DomainElement if e.id == id => e
    }

  /** Finds in the nested model structure AmfObjects with the requested types. */
  def findByType(shapeType: String): Seq[DomainElement] = {
    val predicate = { element: DomainElement =>
      metaModel(element).`type`.exists(valueType => valueType.iri() == shapeType)
    }
    iterator().collect { case e: DomainElement if predicate(e) => e }.toSeq
  }

  def transform(selector: DomainElement => Boolean, transformation: (DomainElement, Boolean) => Option[DomainElement])(
      implicit errorHandler: AMFErrorHandler
  ): BaseUnit = {
    val domainElementAdapter  = new DomainElementSelectorAdapter(selector)
    val transformationAdapter = new DomainElementTransformationAdapter(transformation)
    new TransformationTraversal(TransformationData(domainElementAdapter, transformationAdapter)).traverse(this)
    this
  }

  def findInReferences(id: String): Option[BaseUnit] = references.find(_.id == id)

  def sourceSpec: Option[Spec] = processingData.sourceSpecProvider

  protected[amf] def profileName: Option[ProfileName] = sourceSpec.map(v => ProfileName.apply(v.id))

  def withReferenceAlias(alias: String, id: String, fullUrl: String, relativeUrl: String): BaseUnit = {
    val aliasTuple = (alias, ReferencedInfo(id, fullUrl, relativeUrl))
    this.annotations.find(classOf[Aliases]) match {
      case Some(aliases) =>
        this.annotations.reject(_.isInstanceOf[Aliases])
        this.add(aliases.copy(aliases = aliases.aliases + aliasTuple))
      case None => this.add(Aliases(Set(aliasTuple)))
    }
    this
  }

  def cloneUnit(): BaseUnit = {
    cloneElement(mutable.Map.empty).asInstanceOf[BaseUnit]
  }

  override def cloneElement(branch: mutable.Map[AmfObject, AmfObject]): AmfObject = {
    val cloned = super.cloneElement(branch).asInstanceOf[BaseUnit]
    cloned.raw = raw
    cloned
  }
}
