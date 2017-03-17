package inquisitor.io

import java.nio.file.DirectoryIteratorException
import java.nio.file.{ Files => JFiles }
import java.nio.file.Path
import java.nio.file.Paths

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.immutable.Stream

import com.typesafe.scalalogging.StrictLogging

object Files extends StrictLogging {
  import Stream._

  def readDirectory(path: Path): Seq[Path] = {
    if (!isDirectory(path)) { logger.warn(s"File $path is not a directory"); Seq() }

    try {
      val directoryStream = JFiles.newDirectoryStream(path)
      try {
        val s = directoryStream.asScala.toList
        logger.info(s"Reading $path")
        s
      } catch {
        case e: DirectoryIteratorException => {
          logger.warn(s"Error occurred while reading directory $path")
          logger.debug("", e.getCause)
          Seq()
        }
      } finally {
        directoryStream.close
      }
    } catch {
      case e: Exception => {
        logger.warn(s"Could not open directory $path}")
        logger.debug("", e)
        Seq()
      }
    }
  }

  def getDirectoryStream(b: Seq[Path]): Stream[Path] = {
    def bazHelper(s: Seq[Path], dirs: List[Path]): Stream[Path] = {
      (s, dirs) match {
        case (Nil, Nil)          => Stream.empty
        case (Nil, head :: tail) => bazHelper(readDirectory(head), tail)
        case (x, y) => {
          val (directories, others) = x.view.partition(isDirectory(_))
          others.filter(isRegularFile(_)).toStream #::: bazHelper(Nil, directories.toList ::: y)
        }
      }
    }
    bazHelper(b, Nil)
  }

  def isRegularFile(path: Path) = JFiles.isRegularFile(path)

  def isDirectory(path: Path) = JFiles.isDirectory(path)

  def exists(path: String) = JFiles.exists(Paths.get(path))
}
