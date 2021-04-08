package amf.client.remod.amfcore.config

import amf.core.model.document.BaseUnit

private[amf] trait AMFEventListener {
  def event(eventKind: EventKind, bu: BaseUnit)
}

sealed case class EventKind(name: String)

object ParseBeginEventClass extends EventKind("PARSE_BEGIN")

object ParseEndEventClass extends EventKind("PARSE_END")
