package amf.core.internal.remote

trait FileMediaType {
  def mimeFromExtension(extension: String): Option[String] =
    extension match {
      case "graphql" | "gql"               => Some(Mimes.`application/graphql`)
      case "proto" | "pb"                  => Some(Mimes.`application/x-protobuf`)
      case "json"                          => Some(Mimes.`application/json`)
      case "yaml" | "yam" | "yml" | "raml" => Some(Mimes.`application/yaml`)
      case "jsonld" | "amf"                => Some(Mimes.`application/ld+json`)
      case "nt"                            => Some(Mimes.`text/n3`)
      case _                               => None
    }

  def extension(path: String): Option[String] = {
    Some(path.lastIndexOf(".")).filter(_ > 0).map(dot => path.substring(dot + 1))
  }
}

object FileMediaType extends FileMediaType
