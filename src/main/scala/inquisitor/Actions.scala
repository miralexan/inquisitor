package inquisitor

import com.typesafe.scalalogging.StrictLogging
import inquisitor.xml.Model._
import inquisitor.db.Queries._
import inquisitor.io.Files
import inquisitor.db.Schema._
import java.io.File
import slick.driver.SQLiteDriver.api._
import com.lucidchart.open.xtract.XmlReader
import scala.xml.XML
import java.nio.file.Paths
import scala.concurrent.duration._
import scala.concurrent.Await
import com.lucidchart.open.xtract.ParseError
import java.net.URI
import scala.concurrent.Future

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
    val foo = getTestResultsInStatus().map(tcs => if (tcs.isEmpty) logger.info("No unaccounted errors!") else tcs.foreach(tc => logger.warn(s"No successes recorded for ${tc.className}#${tc.name}")))
    await(execute(foo)(c))
  }

  val testResultReader: PartialFunction[File, Seq[TestResult]] = {
    case x if x.getName.endsWith(".xml") => XmlReader.of[TestSuite].read(XML.loadFile(x)).fold(logParseError(x)_)(logParseSuccess(x)_)
  }

  def await[T](future: Future[T]) = Await.result(future, 30 seconds)

  def execute[T](dbio: DBIOAction[T, NoStream, Effect.All])(config: Config) = {
    val db = Database.forURL(buildSqliteUrl(config.outFile), driver = "org.sqlite.JDBC")
    val databaseExecution = db.run(dbio)
    databaseExecution.onComplete(_ => db.close())
    databaseExecution
  }

  def buildSqliteUrl(f: File) = new URI("jdbc:sqlite", f.toString(), null).toString()

  def getTestCaseMap(rows: Traversable[(Int, String, String)]) =
    if (rows.isEmpty) Map[TestCase, Int]()
    else rows.map { case (id, clazz, name) => Map((TestCase(clazz, name), id)) }.reduce(_ ++ _)

  def getExistingTestCaseIds(tcs: Seq[TestCase]) = getTestCaseIds(tcs.toSet).result.map(getTestCaseMap(_))

  def getOrInsertTestCases(tcs: Seq[TestCase]) = getExistingTestCaseIds(tcs).flatMap { f =>
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
