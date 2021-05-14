package amf.client.interface.config

import scala.scalajs.js.annotation.JSExportAll

/**
  * Defines an event listener linked to a specific {@link amf.client.interface.config.AMFEvent}
  */
@JSExportAll
trait AMFEventListener {
  def notifyEvent(event: AMFEvent)
}

private[amf] case class AMFEventListenerClientConverter(clientListener: AMFEventListener) extends AMFEventListener {
  override def notifyEvent(event: AMFEvent): Unit = {
    val clientEvent = AMFEventConverter.asClient(event)
    clientListener.notifyEvent(clientEvent)
  }
}
