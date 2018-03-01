package inquisitor.model

import java.time.Duration

/** Represents the result of a test execution. */
sealed trait ResultType {
  def message: String
}

/** Represents a successful test execution. */
case object Success extends ResultType {
  val message = ""
}

/** Represents a skipped test execution.
  * @param message The possibly empty message indicating why the test was skipped.
  */
case class Skipped(message: String) extends ResultType

/** Represents a failed test execution.
  * @param message The possibly empty message indicating why the test failed.
  */
case class Failure(message: String) extends ResultType

/** Represents a test case terminated by an execution error.
  * @param message The possibly empty message indicating what caused the test to abort.
  */
case class Error(message: String) extends ResultType

/** Represents an individual test case.
  * @param className The class in which the test case is defined.
  * @param name The name of the test case.
  */
case class TestCase(className: String, name: String)

/** Represents an individual test execution.
  * @param testCase The [[TestCase test case]] that was executed.
  * @param time The amount of time it took the test to execute.
  * @param resultType The [[ResultType result]] of the test execution.
  */
case class TestResult(testCase: TestCase, time: Duration, resultType: ResultType)

/** Container for a collection of [[TestResult test results]].
  * @param testResults The possibly empty test results contained in this suite.
  */
case class TestSuite(testResults: Seq[TestResult])
