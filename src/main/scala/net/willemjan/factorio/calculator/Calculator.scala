package net.willemjan.factorio.calculator

import java.io.File

import net.willemjan.factorio.model._

import scala.concurrent.duration.{Duration, DurationInt}

class Calculator {

  private def parse() = {
    // @TODO Add Loader to find factorio installation on multiple platforms (Steam/Regular - Windows/Linux/Mac)
    val parser = new FactorioParser(
      new File("C:/Program Files (x86)/Steam/steamapps/common/Factorio/data"),
      new File("C:/Users/wjzijderveld/AppData/Roaming/Factorio/mods")
    )

    parser.parse()
  }

  def calculate(name: String, count: Integer): Unit = calculate(name, count, 1.seconds)
  def calculate(name: String, count: Integer, duration: Duration): Unit = {
    val data = parse()

    println(data.recipes.get(name))

    data.recipes.get(name) match {
      case Some(recipe) => data.assemblingMachines.filter { item => item._2.categories.contains(recipe.category)} foreach { machine => makeBuild(count, duration, recipe, machine._2) }
      case None => throw new Exception(s"Unable to find recipe for $name")
    }
  }

  private def makeBuild(count: Integer, duration: Duration, recipe: Recipe, assemblingMachine: AssemblingMachine) = {
    val perMachinePerSecond = recipe.requiredEnergy * assemblingMachine.speed
    val countPerSecond = duration.toSeconds * count

    println(f"${countPerSecond / perMachinePerSecond}%1.2f x ${assemblingMachine.name}")
  }
}

object Calculator {
  def apply() = new Calculator
}
