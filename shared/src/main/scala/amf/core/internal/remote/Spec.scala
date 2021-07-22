package amf.core.internal.remote

class Spec(name: String, version: String) {
  def id: String = s"$name + $version".trim
}

object Specs {
  case object Raml08  extends Spec("RAML", "0.8")
  case object Raml10  extends Spec("RAML", "1.0")
  case object Oas20   extends Spec("OAS", "2.0")
  case object Oas30   extends Spec("OAS", "3.0")
  case object Async20 extends Spec("ASYNC", "2.0")
}
