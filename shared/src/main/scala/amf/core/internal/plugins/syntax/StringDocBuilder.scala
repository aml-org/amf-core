package amf.core.internal.plugins.syntax

import amf.core.client.scala.parse.document.{ParsedDocument, StringParsedDocument}
import amf.core.internal.plugins.syntax.StringDocBuilder.INDENTATION_WIDTH
import org.mulesoft.common.client.lexical.Position
import org.mulesoft.common.client.lexical.Position.{FIRST, ZERO}

import scala.collection.mutable

class SourceCodeBlock(val indentation: Integer, val lines: mutable.Buffer[(String, Position)] = mutable.Buffer()) {

  def sortedBuilder: StringBuilder = {
    val b            = new StringBuilder()
    var previousLine = minPos.line
    lines.sortBy(_._2).foreach { case (l, p) =>
      while (previousLine + 1 < p.line) {
        b.append("\n")
        previousLine += 1
      }
      b.append(s"${l}\n")
      val newLines = l.count(c => c == '\n')
      previousLine += (newLines + 1)
    }
    b
  }

  def sortedBuilderWithDelimiter(delimiter: String): StringBuilder = {
    val b = new StringBuilder()
    lines.sortBy(_._2).map(_._1).zipWithIndex.foreach { case (l, idx) =>
      if (idx < lines.length - 1) {
        b.append(s"$l$delimiter")
      } else {
        b.append(l)
      }
    }
    b
  }

  private def countOccurrences(src: String, tgt: String): Int = src.sliding(tgt.length).count(window => window == tgt)

  def builder: StringBuilder = {
    val b = new StringBuilder()
    lines.foreach { case (l, _) =>
      b.append(s"$l\n")
    }
    b
  }

  def minPos: Position = {
    val positions = lines.filter { case (_, p) =>
      p != ZERO && p != FIRST
    }
    positions.map(_._2).sorted.headOption.getOrElse(ZERO)
  }

  def +=(s: String, pos: Position = Position.ZERO): SourceCodeBlock = {
    lines.append((s"${" " * indentation}${s}", pos))
    this
  }

  def merge(s: String, pos: Position = Position.ZERO): SourceCodeBlock = {
    lines.append((s, pos))
    this
  }
}

object SourceCodeBlock {
  def apply(): SourceCodeBlock = new SourceCodeBlock(0)
}

object StringDocBuilder {
  val INDENTATION_WIDTH = 2
}

class StringDocBuilder(document: SourceCodeBlock = SourceCodeBlock()) extends ASTBuilder[SourceCodeBlock] {

  def inilined(f: StringDocBuilder => Unit): String = {
    val cb = new SourceCodeBlock(0)
    val b  = new StringDocBuilder(cb)
    f(b)
    cb.builder.toString().replaceAll("\n", "")
  }

  def listWithDelimiter(delimiter: String)(f: StringDocBuilder => Unit) = {
    val sb               = new SourceCodeBlock(document.indentation)
    val sortedDocBuilder = new StringDocBuilder(sb)
    f(sortedDocBuilder)
    val s   = sb.sortedBuilderWithDelimiter(delimiter).toString()
    val pos = sb.minPos
    merge(s, pos)
    this
  }

  def list(f: StringDocBuilder => Unit): StringDocBuilder = {
    val sb               = new SourceCodeBlock(document.indentation)
    val sortedDocBuilder = new StringDocBuilder(sb)
    f(sortedDocBuilder)
    val s   = sb.sortedBuilder.toString().dropRight(1)
    val pos = sb.minPos
    merge(s, pos)
    this
  }

  def fixed(f: StringDocBuilder => Unit): StringDocBuilder = {
    val sb              = new SourceCodeBlock(document.indentation)
    val fixedDocBuilder = new StringDocBuilder(sb)
    f(fixedDocBuilder)
    val s   = sb.builder.toString().dropRight(1)
    val pos = sb.minPos
    merge(s, pos)
    this
  }

  def obj(f: StringDocBuilder => Unit): StringDocBuilder = {
    val sb               = new SourceCodeBlock(document.indentation + INDENTATION_WIDTH)
    val nestedDocBuilder = new StringDocBuilder(sb)
    f(nestedDocBuilder)
    val s   = sb.builder.toString().dropRight(1)
    val pos = sb.minPos
    merge(s, pos)
    this
  }

  def doc(f: StringDocBuilder => Unit): StringDocBuilder = {
    val sb  = new SourceCodeBlock(0, document.lines)
    val sdb = new StringDocBuilder(sb)
    f(sdb)
    sdb
  }

  def +=(s: String, pos: Position = Position.ZERO): SourceCodeBlock = document.+=(s, pos)

  def merge(s: String, pos: Position = Position.ZERO): SourceCodeBlock = document.merge(s, pos)

  /** Return the result document */
  override def astResult: SourceCodeBlock = document

  override def parsedDocument: ParsedDocument = StringParsedDocument(document)
}
