package inquisitor.db

import slick.jdbc.SQLiteProfile.api._
import java.time.Duration
import inquisitor.xml.Model._

object Schema {

  val testCases = TableQuery[TestCases]

  class TestCases(tag: Tag) extends Table[(Int, String, String)](tag, "TEST_CASES") {
    def testId = column[Int]("TEST_ID", O.PrimaryKey, O.AutoInc)
    def testClass = column[String]("TEST_CLASS")
    def name = column[String]("NAME")
    def * = (testId, testClass, name)

    def idx = index("idx", (testClass, name), unique = true)
  }

  val testResults = TableQuery[TestResults]

  object ResultTypeFlag extends Enumeration {
    type ResultTypeFlag = Value

    val success = Value(0)
    val failed = Value(1)
    val error = Value(2)
    val skipped = Value(3)

    def fromResultType(resultType: ResultType) = resultType match {
      case Success    => ResultTypeFlag.success
      case Failure(_) => ResultTypeFlag.failed
      case Error(_)   => ResultTypeFlag.error
      case Skipped(_) => ResultTypeFlag.skipped
    }

    def toResultType(flag: ResultTypeFlag, msg: String) = flag match {
      case `success` => Success
      case `failed`  => Failure(msg)
      case `error`   => Error(msg)
      case `skipped` => Skipped(msg)
    }
  }

  import ResultTypeFlag._
  implicit val resultTypeFlagMapping = MappedColumnType.base[ResultTypeFlag, Int](_.id, ResultTypeFlag.apply _)
  implicit val durationMapping = MappedColumnType.base[Duration, String](_.toString, Duration.parse(_))

  class TestResults(tag: Tag) extends Table[(Int, Int, ResultTypeFlag, String, Duration)](tag, "TEST_RESULTS") {

    def testResultId = column[Int]("RESULT_ID", O.PrimaryKey, O.AutoInc)
    def testId = column[Int]("TEST_ID")
    def resultTypeFlag = column[ResultTypeFlag]("RESULT_TYPE_FLAG")
    def message = column[String]("MESSAGE")
    def duration = column[Duration]("DURATION")
    def * = (testResultId, testId, resultTypeFlag, message, duration)

    def testCase = foreignKey("TEST_CASE_FK", testId, testCases)(_.testId)

  }
}
