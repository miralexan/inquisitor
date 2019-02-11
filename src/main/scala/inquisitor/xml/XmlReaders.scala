package inquisitor.xml

import java.text.NumberFormat
import java.time.Duration
import java.util.Locale

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

  private def readDuration(text: String): Duration = {
    val parser = NumberFormat.getInstance(Locale.ENGLISH)

    val parsed = parser.parse(text)
    val millis = parsed.doubleValue() * 1000
    Duration.ofMillis(millis.asInstanceOf[Long])
  }

  private implicit val testResultReader: XmlReader[TestResult] = (
    __.read[TestCase] and attribute[String]("time").map(readDuration) and
      __.read[ResultType])(TestResult.apply _)

  private val eclipseCase: XmlReader[TestSuite] = (__ \ "testcase").read(seq[TestResult].atLeast(1)).map(TestSuite.apply)

  private val eclipseSuite: XmlReader[TestSuite] = eclipseCase.orElse((__ \ "testsuite").read(seq(eclipseSuite).atLeast(1)).map(suites => TestSuite(suites.flatMap(s => s.testResults))))

  private val eclipse: XmlReader[TestSuite] = (__ \ "testsuite").read(seq(eclipseSuite).atLeast(1)).map(suites => TestSuite(suites.flatMap(s => s.testResults)))

  private val surefire: XmlReader[TestSuite] = (__ \ "testcase").read(seq[TestResult].atLeast(1)).map(TestSuite.apply)

  implicit val reader: XmlReader[TestSuite] = eclipse.orElse(surefire)
}
