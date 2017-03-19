package inquisitor.db

import slick.driver.SQLiteDriver.api._
import scala.concurrent.ExecutionContext
import slick.sql.FixedSqlAction
import inquisitor.xml.Model._
import inquisitor.db.Schema._

object Queries {

  def getTestCaseIds()(implicit ec: ExecutionContext) = testCases.result.map { tcs =>
    if (tcs.isEmpty) Map.empty[TestCase, Int]
    else tcs.foldLeft(Map.empty[TestCase, Int]) { case (map, (id, clazz, name)) => map + ((TestCase(clazz, name), id)) }
  }

  def insertTestCases(tcs: Iterable[TestCase])(implicit ec: ExecutionContext) =
    ((testCases returning testCases.map(_.testId) into ((tc, id) => (id, tc._2, tc._3))) ++= tcs.map(tc => (0, tc.className, tc.name)))

  def insertTestResults(trs: Iterable[TestResult], f: TestResult => Int)(implicit ec: ExecutionContext) =
    testResults ++= trs.map(tr => (0, f(tr), ResultTypeFlag.fromResultType(tr.resultType), tr.resultType.message, tr.time))

  def getTestCasesWithNoSuccesses()(implicit ec: ExecutionContext) =
    testCases.filter { x => testResults.filter(y => y.testId === x.testId && y.resultTypeFlag.inSet(ResultTypeFlag.success + ResultTypeFlag.skipped)).size === 0 }
      .sortBy(tc => (tc.testClass.asc, tc.name.asc)).result.map(_.map { case (_, clazz, name) => TestCase(clazz, name) })
}
