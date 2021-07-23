package amf.core.internal.remote

import amf.core.internal.remote.Mimes._

/**
  * Syntax
  */
object Syntax {

  sealed trait Syntax {
    val extension: String
    val mediaType: String
  }

  case object Yaml extends Syntax {
    override val extension: String = "yaml"
    override val mediaType: String = `application/yaml`
  }
  case object Json extends Syntax {
    override val extension: String = "json"
    override val mediaType: String = `application/json`
  }

  case object JsonLd extends Syntax {
    override val extension: String = "jsonld"
    override val mediaType: String = `application/ld+json`
  }

  case object PlainText extends Syntax {
    override val extension: String = "txt"
    override val mediaType: String = "text/plain"
  }

  private val yamlMimes = Set(
      `text/yaml`,
      `text/x-yaml`,
      `text/vnd.yaml`,
      `application/yaml`,
      `application/x-yaml`,
      `application/raml+yaml`,
      `application/openapi+yaml`,
      `application/swagger+yaml`,
      `application/asyncapi+yaml`,
      `application/async+yaml`
  )

  private val jsonMimes = Set(`application/json`,
                              `application/raml+json`,
                              `application/openapi+json`,
                              `application/swagger+json`,
                              `application/asyncapi+json`,
                              `application/async+json`)

  /** Attempt to resolve [[Syntax]] from [[Mimes]]. */
  def unapply(mime: Option[String]): Option[Syntax] = mime match {
    case Some(m) if yamlMimes.contains(m) => Some(Yaml)
    case Some(m) if jsonMimes.contains(m) => Some(Json)
    case _                                => None
  }
}
