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

  case object Protobuf extends Syntax {
    override val extension: String = "proto"
  }

  case object GraphQL extends Syntax {
    override val extension: String = "graphql"
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
    override val mediaType: String = Mimes.`text/plain`
  }

  private val yamlMimes = Set(
      `text/yaml`,
      `text/x-yaml`,
      `text/vnd.yaml`,
      `application/yaml`,
      `application/x-yaml`,
  )

  private val jsonMimes = Set(`application/json`,
                              `application/ld+json`)

  val proto3Mimes  = Set(`APPLICATION/GRPC`, `APPLICATION/GRPC+PROTO`, `APPLICATION/X-PROTOBUF`,
    `APPLICATION/PROTOBUF`, `APPLICATION/PROTOBUF_`, `APPLICATION/VND_GOOGLE`)

  val graphQLMimes  = Set(`APPLICATION/GRAPHQL`)

  /** Attempt to resolve [[Syntax]] from [[Mimes]]. */
  def unapply(mime: Option[String]): Option[Syntax] = mime match {
    case Some(m) if yamlMimes.contains(m) => Some(Yaml)
    case Some(m) if jsonMimes.contains(m) => Some(Json)
    case Some(m) if proto3Mimes.contains(m) => Some(Protobuf)
    case Some(m) if graphQLMimes.contains(m) => Some(GraphQL)
    case _ => None
  }
}
