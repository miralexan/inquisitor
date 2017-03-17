package inquisitor

import inquisitor.Actions._
import java.io.File

object Configuration {
  case class Config(action: Option[Action] = None, evidence: File = null, outFile: File = new File("archive.inq"))

  object Config {
    import scopt._

    def parse(args: Seq[String]) = parser.parse(args, Config())

    private val parser = new OptionParser[Config]("inquisitor") {
      head("inquisitor", "0.0.1")

      help("help").text("Print usage text")

      opt[File]('o', "outputFile").action((f, c) => c.copy(outFile = if (f.isDirectory()) new File(f, "archive.inq") else f))
        .text("The location where inquisitor should store gathered evidence.")

      cmd("create").action((_, c) => c.copy(action = Some(Create)))
        .text("Start the inquisition and record the evidence thus far.")
        .children(
          arg[File]("<evidence>").action((s, c) => c.copy(evidence = s))
            .required()
            .maxOccurs(1)
            .text("The evidence to be recorded, or a location where such evidence may be found.")
            .validate(x => if (x.exists()) success else failure(s"File [$x] does not exist")),
          checkConfig(c => if (c.outFile.exists() && c.action.fold(false)(a => a == Create)) failure(s"File ${c.outFile.getCanonicalPath} already exists!") else success))

      cmd("add").action((_, c) => c.copy(action = Some(Add)))
        .text("Start the inquisition and record the evidence thus far.")
        .children(
          arg[File]("<evidence>").action((s, c) => c.copy(evidence = s))
            .required()
            .maxOccurs(1)
            .text("The evidence to be recorded, or a location where such evidence may be found.")
            .validate(x => if (x.exists()) success else failure(s"File [$x] does not exist")),
          checkConfig(c => if (!c.outFile.exists() && c.action.fold(false)(a => a == Add)) failure(s"File ${c.outFile.getCanonicalPath} does not exist!") else success))

      cmd("validate").action((_, c) => c.copy(action = Some(Validate)))
        .text("Verify the recorded evidence.").children(
          checkConfig(c => if (!c.outFile.exists() && c.action.fold(false)(a => a == Validate)) failure(s"File ${c.outFile.getCanonicalPath} does not exist!") else success))

      checkConfig(c => c.action.fold(failure("No action specified."))(_ => success))
    }
  }
}
