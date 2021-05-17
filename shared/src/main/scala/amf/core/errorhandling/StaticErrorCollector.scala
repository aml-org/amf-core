package amf.core.errorhandling
import amf.core.validation.AMFValidationResult

import scala.collection.mutable

object StaticErrorCollector {

  private val errorsForUnitCount: mutable.Map[String, Seq[AMFValidationResult]] = mutable.Map.empty

  // TODO: ARM remove this when test be fixed
  def collect(result: AMFValidationResult, id: String): Unit = synchronized {
    errorsForUnitCount.get(id) match {
      case Some(seq) => errorsForUnitCount.update(id, result +: seq)
      case None      => errorsForUnitCount.put(id, Seq(result))
    }
  }

  def clean(): Unit = errorsForUnitCount.clear()

  def getRun(id: String): Seq[AMFValidationResult] = errorsForUnitCount.getOrElse(id, Nil)

}
