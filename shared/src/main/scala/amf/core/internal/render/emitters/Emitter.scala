package amf.core.internal.render.emitters

import org.mulesoft.common.client.lexical.Position
import org.yaml.model.YDocument.{EntryBuilder, PartBuilder}

/** Created by pedro.colunga on 8/22/17.
  */
trait Emitter {
  def position(): Position
}

trait PartEmitter extends Emitter {
  def emit(b: PartBuilder): Unit
}

trait EntryEmitter extends Emitter {
  def emit(b: EntryBuilder): Unit
}
