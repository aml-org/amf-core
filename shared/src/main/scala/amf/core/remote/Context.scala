package amf.core.remote

import amf.Core.platform
import amf.core.remote.Context.resolveRelativeTo

/**
  * Context class for URL resolution.
  */
class Context protected(val platform: Platform,
                        val history: List[String]) {

  def hasCycles: Boolean = history.count(_.equals(current)) == 2

  def current: String = if (history.isEmpty) "" else history.last
  def root: String    = if (history.isEmpty) "" else history.head

  def update(url: String): Context = Context(platform, history, resolve(url))

  def resolve(url: String): String =
    try {
      resolvePathAccordingToRelativeness(url)
    } catch {
      case e: Exception => throw new PathResolutionError(s"url: $url - ${e.getMessage}")
    }

  private def resolvePathAccordingToRelativeness(url: String) = {
    val base = url match {
      case Absolute(_)               => None
      case RelativeToRoot(_)         => Some(root)
      case RelativeToIncludedFile(_) => Some(current)
    }
    resolveRelativeTo(base, url)
  }

}

object Context {
  private def apply(platform: Platform,
                    history: List[String],
                    currentResolved: String): Context =
    new Context(platform, history :+ currentResolved)

  def apply(platform: Platform): Context = new Context(platform, Nil)

  def apply(platform: Platform, root: String): Context =
    new Context(platform, List(root))

  def stripFileName(url: String): String = {
    val withoutFrag = if (url.contains("#")) url.split("#").head else url

    val so = platform.operativeSystem()
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

  def resolveRelativeTo(base: Option[String], url: String): String = {
    val result = base.map { baseUri =>
      if (url.startsWith("#")) baseUri + url
      else {
        val baseDir = Context.stripFileName(baseUri)
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
}

object Absolute {
  def unapply(url: String): Option[String] = url match {
    case s if s.contains(":") => Some(s)
    case _                    => None
  }
}

object RelativeToRoot {
  def unapply(url: String): Option[String] = url match {
    case s if s.startsWith("/") => Some(s)
    case _                      => None
  }
}

object RelativeToIncludedFile {
  def unapply(url: String): Option[String] = Some(url)
}
