package amf.core.internal.utils

import amf.core.internal.unsafe.PlatformSecrets

import java.net.URI

object UriUtils extends PlatformSecrets {

  def resolveRelativeTo(current: String, rawUrl: String): String = {
    val base = rawUrl match {
      case Absolute(_)               => None
      case RelativeToIncludedFile(_) => Some(current)
    }
    val encodedUrl = platform.encodeURI(rawUrl)
    resolveWithBase(base, encodedUrl)
  }

  def resolveWithBase(base: Option[String], url: String): String = {
    val result = base
      .map { baseUri =>
        if (url.startsWith("#")) baseUri + url
        else {
          val baseDir = stripFileName(baseUri)
          safeConcat(baseDir, url)
        }
      }
      .getOrElse(url)
    resolvePath(result)
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

    val containsBackSlash    = withoutFrag.contains('\\') && so == "win"
    val containsForwardSlash = withoutFrag.contains('/')
    if (!containsBackSlash && !containsForwardSlash) {
      return ""
    }
    val sep                   = if (containsBackSlash) '\\' else '/'
    val lastPieceHasExtension = withoutFrag.split(sep).last.contains('.')
    if (lastPieceHasExtension) {
      withoutFrag.substring(0, withoutFrag.lastIndexOf(sep) + 1)
    } else if (!withoutFrag.endsWith(sep.toString)) {
      withoutFrag + sep
    } else {
      withoutFrag
    }
  }

  /** normalize path method for file fetching in amf compiler */
  def normalizePath(url: String): String = fixFilePrefix(normalizeWithPrefixManagement(platform.encodeURI(url)))

  /** Test path resolution. */
  def resolvePath(path: String): String = {
    val normalized = normalizeWithPrefixManagement(path)
    fixFilePrefix(normalized)
  }

  private def normalizeWithPrefixManagement(path: String): String = {
    path match {
      case Absolute(_) =>
        // The prefixes, in some cases (when multiple), generate problems with the path normalization. We remove them, normalize, and then add them again.
        val (prefix, rawPath) = path.splitAt(path.lastIndexOf(':') + 1)
        prefix + new URI(rawPath).normalize.toString
      case _ => new URI(path).normalize.toString
    }
  }

  private def fixFilePrefix(res: String): String = {
    if (res.startsWith("file://") || res.startsWith("file:///")) {
      res
    } else if (res.startsWith("file:/")) {
      res.replace("file:/", "file:///")
    } else {
      res
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
