package amf.core.internal.remote.fs

import org.mulesoft.common.io.{AsyncFile, FileSystem, SyncFile}

/** Unsupported file system. */
object UnsupportedFileSystem extends FileSystem {

  override def syncFile(path: String): SyncFile = unsupported

  override def asyncFile(path: String): AsyncFile = unsupported

  override def separatorChar: Char = unsupported

  private def unsupported = throw new Exception(s"Unsupported operation")
}
