package amf.core.utils

import amf.Core.platform

object UriUtils {

  def resolveRelativeTo(current: String, rawUrl: String): String = {
    val base = rawUrl match {
      case Absolute(_)               => None
      case RelativeToIncludedFile(_) => Some(current)
    }
    val encodedUrl = platform.encodeURI(rawUrl)
    resolve(base, encodedUrl)
  }

  def resolve(base: Option[String], url: String): String = {
    val result = base.map { baseUri =>
      if (url.startsWith("#")) baseUri + url
      else {
        val baseDir = stripFileName(baseUri)
        safeConcat(baseDir, url)
      }
    }.getOrElse(url)
    platform.resolvePath(result)
  }

  private def safeConcat(base: String, url: String) = {
    if (base.nonEmpty && base.last == '/' && url.nonEmpty && url.head == '/') base + url.drop(1)
    else if ((base == "file://") && url.startsWith("./")) base + url.substring(2) // associated to APIMF-2357
    else base + url
  }

  def stripFileName(url: String): String = stripFileName(url, platform.operativeSystem())

  // git history can be found in core.remote.Context
  private[utils] def stripFileName(url: String, so: String): String = {
    val withoutFrag = if (url.contains("#")) url.split("#").head else url

    val containsBackSlash = withoutFrag.contains('\\') && so == "win"
    val containsForwardSlash = withoutFrag.contains('/')
    if (!containsBackSlash && !containsForwardSlash) {
      return ""
    }
    val sep = if (containsBackSlash) '\\' else '/'
    val lastPieceHasExtension = withoutFrag.split(sep).last.contains('.')
    if (lastPieceHasExtension) {
      withoutFrag.substring(0, withoutFrag.lastIndexOf(sep) + 1)
    } else if (!withoutFrag.endsWith(sep.toString)) {
      withoutFrag + sep
    } else {
      withoutFrag
    }
  }
}

object Absolute {
  def unapply(url: String): Option[String] = url match {
    case s if s.contains(":") => Some(s)
    case _                    => None
  }
}

// this applies for RAML
object RelativeToRoot {
  def unapply(url: String): Option[String] = url match {
    case s if s.startsWith("/") => Some(s)
    case _                      => None
  }
}

object RelativeToIncludedFile {
  def unapply(url: String): Option[String] = Some(url)
}
