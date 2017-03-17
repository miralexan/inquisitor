package inquisitor.xml

import java.time.Duration

import com.lucidchart.open.xtract.XmlReader

object Model {
  import XmlReader._
  import com.lucidchart.open.xtract.__
  import play.api.libs.functional.syntax._

  case class TestResult(testCase: TestCase, time: Duration, resultType: ResultType)

  object TestResult {
    implicit val reader: XmlReader[TestResult] = (
      (__).read[TestCase] and attribute[String]("time").map { x => Duration.parse(s"PT${x}S") } and
      (__).read[ResultType])(apply _)
  }

  sealed trait ResultType {
    def message: String
  }

  object ResultType {
    implicit val reader: XmlReader[ResultType] = {
      (__ \ "failure").read[String].map(Failure(_)).orElse {
        (__ \ "error").read[String].map(Error(_)).orElse {
          (__ \ "skipped").read[String].map(Skipped(_)).orElse {
            XmlReader.pure(Success)
          }
        }
      }
    }
  }

  case object Success extends ResultType {
    val message = ""
  }
  case class Skipped(message: String) extends ResultType
  case class Failure(message: String) extends ResultType
  case class Error(message: String) extends ResultType

  case class TestCase(className: String, name: String)

  object TestCase {
    implicit val reader: XmlReader[TestCase] = (attribute[String]("classname") and attribute[String]("name"))(apply _)
  }

  case class TestSuite(testResults: Seq[TestResult])

  object TestSuite {
    val eclipse = ((__ \ "testsuite" \ "testcase")).read(seq[TestResult].atLeast(1)).map(apply _)

    val surefire = ((__ \ "testcase")).read(seq[TestResult].atLeast(1)).map(apply _)

    implicit val reader: XmlReader[TestSuite] = eclipse.orElse(surefire)
  }
}
