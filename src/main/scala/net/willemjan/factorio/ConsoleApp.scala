package net.willemjan.factorio

import net.willemjan.factorio.calculator.Calculator

object ConsoleApp extends App {

  private def exit(code: Int) = System.exit(code)
  private def printUsage() = Console.println(s"${Console.RED_B}You're on your own for now...${Console.RESET}")

  lazy val calculator = Calculator()

  def run(args: List[String]): Unit = args match {
    case ("--help") :: tail =>
      printUsage()
      exit(0)
    case (count) :: (name) :: tail =>
      calculator.calculate(name, count.toDouble)
      exit(0)
    case _ =>
      printUsage()
      exit(1)
  }
  run(args.toList)
}
