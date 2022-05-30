package amf.core.internal

import amf.core.client.common.position
import amf.core.client.common.position.{Position, Range}
import amf.core.internal.remote.Mimes._
import amf.core.internal.unsafe.PlatformSecrets
import org.mulesoft.common.core._
import org.mulesoft.lexer.InputRange
import org.yaml.model.YNode
import org.yaml.model.YNode.Alias

import scala.annotation.tailrec
import scala.util.matching.Regex

package object utils {

  object Regexes {
    lazy val Path: Regex = "(.*)/(.*)".r // Path(parent,child)
  }

  implicit class MediaTypeMatcher(val content: String) extends AnyVal {

    def guessMediaType(isScalar: Boolean): String = { // move to objects
      if (isXml && !isScalar) `application/xml`
      else if (isJson && !isScalar) `application/json`
      else `application/yaml` // by default, we will try to parse it as YAML
    }

    def isXml: Boolean = content.trim.startsWith("<")

    def isJson: Boolean = content.trim.startsWith("{") || content.startsWith("[")
  }

  /** Common utility methods to deal with Strings.
    */
  implicit class AmfStrings(val str: String) extends PlatformSecrets {

    def validReferencePath: Boolean = str.split("\\.").length < 3

    /** Prepend correct protocol prefix, then encode */
    def normalizeUrl: String = {
      val protocolList = Set("http:", "https:", "file:", "jar:")
      if (str == null || str.isEmpty) ""
      else if (protocolList.exists(str.startsWith)) str
      else if (str.startsWith("/")) "file:/" + str
      else "file://" + str
    }

    def noSpaces: String = str.replace(" ", "")

    /** normalize and check that the string its a valid URI */
    def normalizePath: String = UriUtils.normalizePath(str)

    /** Url encoded string. */
    def urlComponentEncoded: String = platform.encodeURIComponent(str)

    /** Url component decoded string. */
    def urlComponentDecoded: String = {
      platform.safeDecodeURIComponent(str) match {
        case Right(decodedUrl) => decodedUrl
        case Left(rawUrl)      => rawUrl
      }
    }

    def urlEncoded: String = platform.encodeURI(str)

    def urlDecoded: String = platform.decodeURI(str)

    def escape: String = {
      val result = new StringBuilder()
      for {
        c <- str
      } {
        result.append(c match {
          case '\n'             => "\\n"
          case '"'              => "\""
          case _ if c.isControl => "\\u" + Integer.toHexString(c)
          case _                => c
        })
      }
      result.toString()
    }

    def asRamlAnnotation = s"(amf-$str)"

    def asOasExtension = s"x-amf-$str"

    def asAsyncExtension = s"x-amf-$str"

    def option: Option[String] = if (str.isEmpty) None else Option(str)

    def toCamelCase: String = {
      val words = str.split("\\s+")
      if (words.size == 1) {
        words.head
      } else {
        (Seq(words.head) ++ words.tail.map(_.capitalize)).mkString
      }
    }
  }

  case class QName(qualification: String, name: String) {
    val isQualified: Boolean = qualification.nonEmpty
    val isEmpty: Boolean     = qualification.isEmpty && name.isEmpty
  }

  object QName {

    def apply(fqn: String): QName = {
      if (fqn.notNull.trim.nonEmpty) {
        fqn.lastIndexOf('.') match {
          case -1  => QName("", fqn)
          case dot => QName(fqn.substring(0, dot), fqn.substring(dot + 1))
        }
      } else Empty
    }

    val Empty: QName = QName("", "")
  }

  object TemplateUri {
    def isValid(chars: String): Boolean = {
      @tailrec
      def isBalanced(cs: List[Char], level: Int): Boolean = cs match {
        case Nil                   => level == 0
        case '}' :: _ if level < 1 => false
        case '}' :: xs             => isBalanced(xs, level - 1)
        case '{' :: xs             => isBalanced(xs, level + 1)
        case _ :: xs               => isBalanced(xs, level)
      }
      isBalanced(chars.toList, 0)
    }

    def invalidMsg(uri: String): String = s"'$uri' is not a valid template uri."

    val varPattern: Regex = "\\{(.[^{]*)\\}".r
    def variables(path: String): Seq[String] =
      varPattern.findAllIn(path).toSeq.map(v => v.replace("{", "").replace("}", ""))
  }

  /** We need to generate unique IDs for all data nodes if the name is not set
    */
  class IdCounter {
    private var c = 0

    def genId(id: String): String = {
      c += 1
      s"${id}_$c"
    }

    def reset(): Unit = c = 0
  }

  implicit class RangeOps(val range: InputRange) extends AnyVal {
    def toOffset: Position = Position(range.lineFrom, range.columnFrom)
    def toRange: Range     = position.Range(range)
  }

  /** Common utility to convert regex into common java grounds.
    */
  implicit class RegexConverter(val str: String) {

    /** Hack: Manipulate regex so that it works the same as in js. Cases:
      *
      * 1) When using "[^]" this means match any character in js, but it is an error in jvm because an empty negated (^)
      * set is not allowed. We replace it with [\S\s] which is the same, it means any character.
      *
      * TODO when to hack opening curly braces? .replaceAll("\\{", "\\\\{")
      */
    def convertRegex: String = str.replaceAll("\\[\\^\\]", "[\\\\S\\\\s]")
  }

  class SimpleCounter() {
    private var current = -1
    def next(): Int = {
      current += 1
      current
    }
  }

  case class AliasCounter(var count: Int = 0, maxThreshold: Int = AliasCounter.defaultThreshold) {
    def exceedsThreshold(node: YNode): Boolean = {
      register(node)
      count > maxThreshold
    }

    def register(node: YNode): Unit = if (node.isInstanceOf[Alias]) count = count + 1
  }

  object AliasCounter {
    val defaultThreshold = 10000
    def apply(maxThreshold: Option[Int]): AliasCounter = maxThreshold match {
      case Some(value) => apply(maxThreshold = value)
      case None        => apply()
    }
  }

}
