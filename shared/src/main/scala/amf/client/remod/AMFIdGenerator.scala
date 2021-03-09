package amf.client.remod

import amf.core.model.domain.DomainElement
import amf.core.utils.IdCounter

trait AMFIdGenerator {

  // get id or set id?
  def id(d: DomainElement, baseUri: String): String

}

// how get parent?
object PathAMFIdGenerator$ extends AMFIdGenerator {
  override def id(d: DomainElement, baseUri: String): String = {
    baseUri + "/" + d.componentId
  }
}

class AutoIncrementAMFIdGenerator() extends AMFIdGenerator {

  // check order for test?

  private val idCounter = new IdCounter()
  override def id(d: DomainElement, baseUri: String): String = {
    idCounter.genId("file://")
  }
}
