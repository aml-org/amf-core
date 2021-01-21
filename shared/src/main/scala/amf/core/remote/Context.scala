package amf.core.remote

import amf.core.utils.{Absolute, RelativeToIncludedFile, RelativeToRoot, UriUtils}

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
    UriUtils.resolve(base, url)
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
}
