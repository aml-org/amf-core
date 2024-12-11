package amf.core.io

import amf.core.common.AsyncFunSuiteWithPlatformGlobalExecutionContext
import org.mulesoft.common.io.{AsyncFile, FileSystem}
import org.mulesoft.common.test.Tests.checkDiff
import org.scalatest.Assertion

import scala.concurrent.Future

trait FileAssertionTest extends AsyncFunSuiteWithPlatformGlobalExecutionContext {

  protected val fs: FileSystem = platform.fs

  protected def writeTemporaryFile(golden: String)(content: String): Future[AsyncFile] = {
    val sanitized = golden.stripPrefix("file://").replaceAll("/", "-")
    val file      = tmp(s"$sanitized.tmp")
    val actual    = fs.asyncFile(file)
    actual.write(content).map(_ => actual)
  }

  protected def assertDifferences(actual: AsyncFile, golden: String): Future[Assertion] = {
    val expected = fs.asyncFile(golden.stripPrefix("file://"))
    expected.read().flatMap(_ => checkDiff(actual, expected))
  }

  private def withTrailingSeparator(dir: String, char: Char): String =
    if (dir.endsWith(char.toString)) dir
    else dir.concat(char.toString)

  /** Return random temporary file name for testing. */
  def tmp(name: String = ""): String =
    withTrailingSeparator(platform.tmpdir(), platform.fs.separatorChar) + System
      .nanoTime() + "-" + name
}
