package amf.core.model.document

import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.config.RenderOptions
import amf.core.annotations.SourceVendor
import amf.core.errorhandling.AMFErrorHandler
import amf.core.metamodel.MetaModelTypeMapping
import amf.core.metamodel.document.BaseUnitModel
import amf.core.metamodel.document.BaseUnitModel.{Location, ModelVersion, Root, Usage}
import amf.core.metamodel.document.DocumentModel.References
import amf.core.model.document.FieldsFilter.Local
import amf.core.model.domain._
import amf.core.model.{BoolField, StrField}
import amf.core.rdf.{RdfModel, RdfModelParser}
import amf.core.remote.{Amf, Vendor}
import amf.core.traversal.iterator._
import amf.core.traversal.{
  DomainElementSelectorAdapter,
  DomainElementTransformationAdapter,
  TransformationData,
  TransformationTraversal
}
import amf.core.unsafe.PlatformSecrets

import scala.collection.mutable

/** Any parseable unit, backed by a source URI. */
trait BaseUnit extends AmfObject with MetaModelTypeMapping with PlatformSecrets {

  // Set the current model version
  withModelVersion("3.1.0")

  // Set the default parsingRoot
  withRoot(false)

  // We store the parser run here to be able to find runtime validations for this model
  private var run: Option[Int]         = None
  protected[amf] var resolved: Boolean = false

  private[amf] def withRunNumber(parserRun: Int): BaseUnit = {
    if (this.run.nonEmpty) // todo: exception or what?
      this
    else {
      this.run = Some(parserRun)
      this
    }
  }

  /** Raw text  used to generated this unit */
  var raw: Option[String] = None

  /** Meta data for the document */
  def meta: BaseUnitModel

  /** Returns the list document URIs referenced from the document that has been parsed to generate this model */
  def references: Seq[BaseUnit]

  /** Returns the file location for the document that has been parsed to generate this model */
  override def location(): Option[String] = {
    val fieldValue: StrField = fields.field(Location)
    fieldValue.option().orElse(super.location())
  }

  /** Returns the usage. */
  def usage: StrField = fields.field(Usage)

  /** Returns true if the base unit is the root of the document model obtained from parsing **/
  def root: BoolField = fields.field(Root)

  /** Returns the version. */
  def modelVersion: StrField = fields.field(ModelVersion)

  /** Set the raw value for the base unit */
  def withRaw(raw: String): BaseUnit = {
    this.raw = Some(raw)
    this
  }

  def withReferences(references: Seq[BaseUnit]): this.type = setArrayWithoutId(References, references)

  def withLocation(location: String): this.type = set(Location, location)

  def withUsage(usage: String): this.type = set(Usage, usage)

  private def withModelVersion(version: String): this.type = set(ModelVersion, version)

  def withRoot(value: Boolean): this.type = set(Root, value)

  def addReference(newRef: BaseUnit): Unit = synchronized(withReferences(references :+ newRef))

  /** Returns Unit iterator for specified strategy and scope. */
  def iterator(strategy: IteratorStrategy = DomainElementStrategy,
               fieldsFilter: FieldsFilter = Local,
               visited: VisitedCollector = IdCollector()): AmfIterator = {
    strategy.iterator(fieldsFilter.filter(fields), visited)
  }

  /**
    * Finds first domain element with the requested id
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
      implicit errorHandler: AMFErrorHandler): BaseUnit = {
    val domainElementAdapter  = new DomainElementSelectorAdapter(selector)
    val transformationAdapter = new DomainElementTransformationAdapter(transformation)
    new TransformationTraversal(TransformationData(domainElementAdapter, transformationAdapter)).traverse(this)
    this
  }

  def findInReferences(id: String): Option[BaseUnit] = references.find(_.id == id)

  def toNativeRdfModel(renderOptions: RenderOptions = RenderOptions()): RdfModel = {
    platform.rdfFramework match {
      case Some(rdf) => rdf.unitToRdfModel(this, renderOptions)
      case None      => throw new Exception("RDF Framework not registered cannot export to native RDF model")
    }
  }

  private[amf] def sourceVendor: Option[Vendor] = this match {
    case e: EncodesModel if Option(e.encodes).isDefined =>
      e.encodes.annotations.find(classOf[SourceVendor]).map(a => a.vendor)
    case d: DeclaresModel => d.annotations.find(classOf[SourceVendor]).map(a => a.vendor)
    case _                => None
  }

  def sourceMediaType: String = sourceVendor.map(_.mediaType).getOrElse(Amf.mediaType)

  def cloneUnit(): BaseUnit = cloneElement(mutable.Map.empty).asInstanceOf[BaseUnit]

  override def cloneElement(branch: mutable.Map[AmfObject, AmfObject]): AmfObject = {
    val cloned = super.cloneElement(branch).asInstanceOf[BaseUnit]
    run.foreach(cloned.withRunNumber)
    cloned.raw = raw
    cloned
  }
}

object BaseUnit extends PlatformSecrets {
  def fromNativeRdfModel(id: String, rdfModel: RdfModel, conf: AMFGraphConfiguration): BaseUnit = {
    RdfModelParser(conf).parse(rdfModel, id)
  }
}
