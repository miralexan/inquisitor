package inquisitor.db

import slick.driver.SQLiteDriver.api._
import scala.concurrent.ExecutionContext
import slick.sql.FixedSqlAction
import inquisitor.xml.Model._
import inquisitor.db.Schema._

object Queries {

  def getTestCaseIds(tcs: Traversable[TestCase]) =
    testCases.filter { f =>
      tcs.map(tc => f.name === tc.name && f.testClass === tc.className).reduceLeft(_ || _)
    }

  def insertTestCases(tcs: Iterable[TestCase])(implicit ec: ExecutionContext) =
    ((testCases returning testCases.map(_.testId) into ((tc, id) => (id, tc._2, tc._3))) ++= tcs.map(tc => (0, tc.className, tc.name)))

  def insertTestResults(trs: Iterable[TestResult], f: TestResult => Int)(implicit ec: ExecutionContext) =
    testResults ++= trs.map(tr => (0, f(tr), ResultTypeFlag.fromResultType(tr.resultType), tr.resultType.message, tr.time))

  def getTestResultsInStatus()(implicit ec: ExecutionContext) =
    testCases.filter { x => testResults.filter(y => y.testId === x.testId && y.resultTypeFlag === ResultTypeFlag.success).size === 0 }.result
      .map(_.map { case (_, clazz, name) => TestCase(clazz, name) })
}
