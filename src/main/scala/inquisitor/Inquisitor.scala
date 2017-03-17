package inquisitor

object Inquisitor extends App {
  Configuration.Config.parse(args).map { c => c.action.map { a => a(c) } }
}
