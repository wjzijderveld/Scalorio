package net.willemjan.factorio.calculator

object Main extends App {

  private def exit(code: Int) = System.exit(code)
  private def printUsage() = Console.println(s"${Console.RED_B}You're on your own for now...${Console.RESET}")

  lazy val calculator = Calculator()

  def run(args: List[String]): Unit = args match {
    case ("--help") :: tail =>
      printUsage()
      exit(0)
    case (count) :: (name) :: tail =>
      calculator.calculate(name, Integer.valueOf(count))
      exit(0)
    case _ =>
      printUsage()
      exit(1)
  }

  run(args.toList)
}
