package amf.core.encoding

import org.yaml.encoder.Encoder

class AmfStringEncoder extends Encoder {
  import org.mulesoft.common.core._
  override def encode(toEncode: String): String = {

    var f = firstEscaped(toEncode)
    if (f == -1) toEncode
    else {
      val out = new StringBuilder(2 * toEncode.length)
      out ++= toEncode.substring(0, f)
      while (f < toEncode.length) {
        val ch = toEncode.charAt(f)
        if (ch < 32) {
          out += '\\'
          ch match {
            case '\b' => out += 'b'
            case '\n' => out += 'n'
            case '\t' => out += 't'
            case '\f' => out += 'f'
            case '\r' => out += 'r'
            case 0    => out += '0'
            case _    => out ++= ch.toHexString
          }
        } else if (ch < 0x7F) {
          if (ch == '"' || ch == '\\') out += '\\'
          out += ch
        } else {
          out += ch
        }
        f += 1
      }
      out.toString
    }
  }

  private def firstEscaped(str: String): Int =
    if (str == null) -1
    else {
      var i      = 0
      val length = str.length
      while (i < length) {
        if (str.charAt(i).needsToBeEscaped) return i
        i += 1
      }
      -1
    }
}
