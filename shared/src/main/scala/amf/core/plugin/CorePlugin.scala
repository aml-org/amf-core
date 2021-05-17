package amf.core.plugin

import amf.client.plugins.{AMFDomainPlugin, AMFPlugin}
import amf.core.annotations.serializable.CoreSerializableAnnotations
import amf.core.entities.CoreEntities
import amf.core.metamodel.Obj
import amf.core.model.domain.AnnotationGraphLoader

import scala.concurrent.{ExecutionContext, Future}

/** Core plugin. No need of registering nor initializing. Used solely for model registry. */
object CorePlugin extends AMFDomainPlugin {

  override def modelEntities: Seq[Obj] = CoreEntities.entities.values.toSeq

  override def serializableAnnotations(): Map[String, AnnotationGraphLoader] = CoreSerializableAnnotations.annotations

  override val ID: String = ""

  override def dependencies(): Seq[AMFPlugin] = Seq.empty

  override def init()(implicit executionContext: ExecutionContext): Future[AMFPlugin] = Future.successful(this)
}
