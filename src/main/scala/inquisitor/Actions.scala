package inquisitor

import java.io.File

import com.lucidchart.open.xtract.{ParseError, XmlReader}
import com.typesafe.scalalogging.StrictLogging
import inquisitor.db.Queries._
import inquisitor.db.Schema._
import inquisitor.io.Files
import inquisitor.model._
import inquisitor.xml.XmlReaders._
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.xml.XML

object Actions extends StrictLogging {
  import Configuration._

  import scala.concurrent.ExecutionContext.Implicits.global

  type Action = Config => Unit

  val Create: Action = config => {
    val setupAction = (testCases.schema ++ testResults.schema).create

    val testResultStream = Files.getDirectoryStream(Seq(config.evidence.toPath())).map(_.toFile()).collect(testResultReader).flatten
    await(execute(setupAction >> insertTestResultsForTestCases(testResultStream))(config)).fold(logger.info("No results could be added."))(x => logger.info(s"Added $x test results"))
  }

  val Add: Action = config => {
    val testResultStream = Files.getDirectoryStream(Seq(config.evidence.toPath())).map(_.toFile()).collect(testResultReader).flatten
    await(execute(insertTestResultsForTestCases(testResultStream))(config)).fold(logger.info("No results could be added."))(x => logger.info(s"Added $x test results"))
  }

  val Validate: Action = c => {
    val query = getTestCasesWithNoSuccesses().map(tcs =>
      if (tcs.isEmpty) logger.info("No unaccounted errors!")
      else {
        logger.warn("No successes recorded for:")
        tcs.groupBy(_.className).foreach {
          case (clazz, tcs) =>
            logger.warn(s"$clazz")
            tcs.foreach(tc => logger.warn(s"\t${tc.name}"))
        }
      })
    await(execute(query)(c))
  }

  val testResultReader: PartialFunction[File, Seq[TestResult]] = {
    case x if x.getName.endsWith(".xml") => XmlReader.of[TestSuite].read(XML.loadFile(x)).fold(logParseError(x)_)(logParseSuccess(x)_)
  }

  def await[T](future: Future[T]) = {
    // Import the postfixOps language feature to keep the compiler from complaining about the use of the `seconds` operator.
    import scala.language.postfixOps
    Await.result(future, 30 seconds)
  }

  def execute[T](dbio: DBIOAction[T, NoStream, Effect.All])(config: Config) = {
    val db = Database.forURL(buildSqliteUrl(config.outFile), driver = "org.sqlite.JDBC")
    val databaseExecution = db.run(dbio.transactionally)
    databaseExecution.onComplete(_ => db.close())
    databaseExecution
  }

  def buildSqliteUrl(f: File) = "jdbc:sqlite:" + f.toString()

  def getTestCaseMap(rows: Traversable[(Int, String, String)]) =
    if (rows.isEmpty) Map.empty[TestCase, Int]
    else rows.foldLeft(Map.empty[TestCase, Int]) { case (map, (id, clazz, name)) => map + ((TestCase(clazz, name), id)) }

  def getOrInsertTestCases(tcs: Seq[TestCase]) = getTestCaseIds().flatMap { f =>
    insertTestCases(tcs.toSet.filter(!f.contains(_))).map(g => f ++ getTestCaseMap(g))
  }

  def insertTestResultsForTestCases(trs: Seq[TestResult]) = getOrInsertTestCases(trs.map(_.testCase)).flatMap { map =>
    insertTestResults(trs, map.compose(_.testCase))
  }

  def logParseSuccess(file: File)(s: TestSuite) = {
    logger.info(s"Successfully parsed file $file")
    s.testResults
  }

  def logParseError(file: File)(pes: Seq[ParseError]) = {
    logger.warn(s"Could not parse file $file")
    logger.debug(pes.toString())
    Seq.empty[TestResult]
  }

}
