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
    override val mediaType: String = "application/yaml"
  }
  case object Json extends Syntax {
    override val extension: String = "json"
    override val mediaType: String = "application/json"
  }

  case object JsonLd extends Syntax {
    override val extension: String = "jsonld"
    override val mediaType: String = "application/ld+json"
  }

  case object PlainText extends Syntax {
    override val extension: String = "txt"
    override val mediaType: String = "text/plain"
  }

  private val yamlMimes = Set(
      `TEXT/YAML`,
      `TEXT/X-YAML`,
      `TEXT/VND.YAML`,
      `APPLICATION/YAML`,
      `APPLICATION/X-YAML`,
      `APPLICATION/RAML+YAML`,
      `APPLICATION/OPENAPI+YAML`,
      `APPLICATION/SWAGGER+YAML`,
      `APPLICATION/ASYNCAPI+YAML`,
      `APPLICATION/ASYNC+YAML`
  )

  private val jsonMimes = Set(`APPLICATION/JSON`,
                              `APPLICATION/RAML+JSON`,
                              `APPLICATION/OPENAPI+JSON`,
                              `APPLICATION/SWAGGER+JSON`,
                              `APPLICATION/ASYNCAPI+JSON`,
                              `APPLICATION/ASYNC+JSON`)

  /** Attempt to resolve [[Syntax]] from [[Mimes]]. */
  def unapply(mime: Option[String]): Option[Syntax] = mime match {
    case Some(m) if yamlMimes.contains(m) => Some(Yaml)
    case Some(m) if jsonMimes.contains(m) => Some(Json)
    case _                                => None
  }
}
