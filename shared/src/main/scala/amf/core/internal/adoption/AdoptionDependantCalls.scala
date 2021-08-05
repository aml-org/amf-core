package amf.core.internal.adoption

/**
  * implemented by all elements which execute setter methods that require element to be adopted.
  */
trait AdoptionDependantCalls {
  private var afterAdoption: Option[() => Unit] = None

  private[amf] def callAfterAdoption(func: () => Unit): Unit = afterAdoption = Some(func)
  private[amf] def run(): Unit = {
    afterAdoption.foreach(_.apply())
    afterAdoption = None
  }
}
