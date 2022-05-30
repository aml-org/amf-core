package amf.core.internal.remote

/** ECMA 3, 15.1.3 URI Handling Function Properties
  *
  * The following are implementations of the algorithms given in the ECMA specification for the hidden functions
  * 'Encode' and 'Decode'.
  */
object EcmaEncoder {
  def encode(str: String, fullUri: Boolean = true): String =
    if (str == null) str
    else {
      var utf8buf: Array[Byte] = null
      var sb: StringBuilder    = null

      val unEscaped = if (fullUri) unEscapedFull else unEscapedPartial

      var k = 0
      while (k < str.length) {
        val C = str(k)
        if (C < 128 && unEscaped(C)) {
          if (sb != null) sb.append(C)
        } else {
          if (sb == null) {
            sb = new StringBuilder(str.length + 3)
            sb.append(str)
            sb.setLength(k)

            utf8buf = Array.ofDim[Byte](6)
          }

          if (0xdc00 <= C && C <= 0xdfff) throw uriError
          var V: Int = 0

          if (C < 0xd800 || 0xdbff < C) {
            V = C
          } else {
            k = k + 1
            if (k == str.length) {
              throw uriError
            }
            val C2: Char = str(k)

            if (!(0xdc00 <= C2 && C2 <= 0xdfff)) {
              throw uriError
            }
            V = ((C - 0xd800) << 10) + (C2 - 0xdc00) + 0x10000
          }
          val L: Int = oneUcs4ToUtf8Char(utf8buf, V)
          var j: Int = 0
          while (j < L) {
            val d: Int = 0xff & utf8buf(j)
            sb.append('%')
            sb.append(toHexChar(d >>> 4))
            sb.append(toHexChar(d & 0xf))
            j = j + 1
          }
        }
        k = k + 1
      }
      if (sb == null) str else sb.toString
    }

  private def toHexChar(i: Int) = {
    if ((i >> 4) != 0) throw uriError // Kit.codeBug
    (if (i < 10) i + '0'
     else i - 10 + 'A').toChar
  }

  /* Convert one UCS-4 char and write it into a UTF-8 buffer, which must be
   * at least 6 bytes long.  Return the number of UTF-8 bytes of data written.
   */
  private def oneUcs4ToUtf8Char(utf8Buffer: Array[Byte], ucs: Int) = {
    var utf8Length = 1

    var ucs4Char = ucs
    if ((ucs4Char & ~0x7f) == 0) utf8Buffer(0) = ucs4Char.toByte
    else {
      var i = 0
      var a = ucs4Char >>> 11
      utf8Length = 2
      while ({
        a != 0
      }) {
        a >>>= 5
        utf8Length += 1
      }
      i = utf8Length
      while ({
        {
          i -= 1; i
        } > 0
      }) {
        utf8Buffer(i) = ((ucs4Char & 0x3f) | 0x80).toByte
        ucs4Char >>>= 6
      }
      utf8Buffer(0) = (0x100 - (1 << (8 - utf8Length)) + ucs4Char).toByte
    }
    utf8Length
  }

  private def uriError = UriError("Bad iri")

  private final val InvalidUtf8: Int = Integer.MAX_VALUE

  private def unHex(c: Char): Int = {
    if ('A' <= c && c <= 'F') c - 'A' + 10
    else if ('a' <= c && c <= 'f') c - 'a' + 10
    else if ('0' <= c && c <= '9') c - '0'
    else -1
  }

  private def unHex(c1: Char, c2: Char): Int = {
    val i1: Int = unHex(c1)
    val i2: Int = unHex(c2)
    if (i1 >= 0 && i2 >= 0) (i1 << 4) | i2
    else -1
  }

  def decode(str: String, fullUri: Boolean = true): String = {
    var buf: Array[Char] = null
    var bufTop           = 0
    var k                = 0
    val length           = str.length
    while ({
      k != length
    }) {
      var C = str.charAt(k)
      if (C != '%') {
        if (buf != null) buf({
          bufTop += 1; bufTop - 1
        }) = C
        k += 1
      } else {
        if (buf == null) { // decode always compress so result can not be bigger then
          // str.length()
          buf = Array.ofDim[Char](length)
          str.getChars(0, k, buf, 0)
          bufTop = k
        }
        val start = k
        if (k + 3 > length) throw uriError
        var B = unHex(str.charAt(k + 1), str.charAt(k + 2))
        if (B < 0) throw uriError
        k += 3
        if ((B & 0x80) == 0) C = B.toChar
        else { // Decode UTF-8 sequence into ucs4Char and encode it into
          // UTF-16
          var utf8Tail    = 0
          var ucs4Char    = 0
          var minUcs4Char = 0
          if ((B & 0xc0) == 0x80) { // First  UTF-8 should be outside 0x80..0xBF
            throw uriError
          } else if ((B & 0x20) == 0) {
            utf8Tail = 1
            ucs4Char = B & 0x1f
            minUcs4Char = 0x80
          } else if ((B & 0x10) == 0) {
            utf8Tail = 2
            ucs4Char = B & 0x0f
            minUcs4Char = 0x800
          } else if ((B & 0x08) == 0) {
            utf8Tail = 3
            ucs4Char = B & 0x07
            minUcs4Char = 0x10000
          } else if ((B & 0x04) == 0) {
            utf8Tail = 4
            ucs4Char = B & 0x03
            minUcs4Char = 0x200000
          } else if ((B & 0x02) == 0) {
            utf8Tail = 5
            ucs4Char = B & 0x01
            minUcs4Char = 0x4000000
          } else { // First UTF-8 can not be 0xFF or 0xFE
            throw uriError
          }
          if (k + 3 * utf8Tail > length) throw uriError
          var j = 0
          while ({
            j != utf8Tail
          }) {
            if (str.charAt(k) != '%') throw uriError
            B = unHex(str.charAt(k + 1), str.charAt(k + 2))
            if (B < 0 || (B & 0xc0) != 0x80) throw uriError
            ucs4Char = (ucs4Char << 6) | (B & 0x3f)
            k += 3

            {
              j += 1; j - 1
            }
          }
          // Check for overlongs and other should-not-present codes
          if (ucs4Char < minUcs4Char || (ucs4Char >= 0xd800 && ucs4Char <= 0xdfff)) ucs4Char = InvalidUtf8
          else if (ucs4Char == 0xfffe || ucs4Char == 0xffff) ucs4Char = 0xfffd
          if (ucs4Char >= 0x10000) {
            ucs4Char -= 0x10000
            if (ucs4Char > 0xfffff) throw uriError
            val H = ((ucs4Char >>> 10) + 0xd800).toChar
            C = ((ucs4Char & 0x3ff) + 0xdc00).toChar
            buf({
              bufTop += 1; bufTop - 1
            }) = H
          } else C = ucs4Char.toChar
        }
        if (fullUri && UriDecodeReserved.indexOf(C) >= 0) {
          var x = start
          while ({
            x != k
          }) {
            buf({
              bufTop += 1; bufTop - 1
            }) = str.charAt(x)

            {
              x += 1; x - 1
            }
          }
        } else
          buf({
            bufTop += 1; bufTop - 1
          }) = C
      }
    }
    if (buf == null) str
    else new String(buf, 0, bufTop)
  }

  private final val UriDecodeReserved = ";/?:@&=+$,#"

  private def encodeUnescaped(c: Int, fullUri: Boolean) = {
    if ('A' <= c && c <= 'Z' || 'a' <= c && c <= 'z' || '0' <= c && c <= '9') true
    else "-_.!~*'()".contains(c) || fullUri && UriDecodeReserved.contains(c)
  }

  private final val unEscapedFull    = ((0 to 128) map (encodeUnescaped(_, fullUri = true))).toArray[Boolean]
  private final val unEscapedPartial = ((0 to 128) map (encodeUnescaped(_, fullUri = false))).toArray[Boolean]

  case class UriError(message: String) extends Exception(message)

}
