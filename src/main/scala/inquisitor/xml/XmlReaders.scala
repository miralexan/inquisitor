package inquisitor.xml

import java.time.Duration

import com.lucidchart.open.xtract.XmlReader
import inquisitor.model._

object XmlReaders {
  import XmlReader._
  import com.lucidchart.open.xtract.__
  import play.api.libs.functional.syntax._

  private implicit val testCaseReader: XmlReader[TestCase] = (
    attribute[String]("classname") and attribute[String]("name"))(TestCase.apply _)

  private implicit val resultTypeReader: XmlReader[ResultType] = {
    (__ \ "failure").read[String].map(Failure).orElse {
      (__ \ "error").read[String].map(Error).orElse {
        (__ \ "skipped").read[String].map(Skipped).orElse {
          XmlReader.pure(Success)
        }
      }
    }
  }

  private implicit val testResultReader: XmlReader[TestResult] = (
    __.read[TestCase] and attribute[String]("time").map { x => Duration.parse(s"PT${x}S") } and
      __.read[ResultType])(TestResult.apply _)

  private val eclipse: XmlReader[TestSuite] = (__ \ "testsuite" \ "testcase").read(seq[TestResult].atLeast(1)).map(TestSuite.apply)

  private val surefire: XmlReader[TestSuite] = (__ \ "testcase").read(seq[TestResult].atLeast(1)).map(TestSuite.apply)

  implicit val reader: XmlReader[TestSuite] = eclipse.orElse(surefire)
}
