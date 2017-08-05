package net.willemjan.factorio.calculator

import java.io.File
import java.util.concurrent.TimeUnit

import net.willemjan.factorio.loader.{FactorioLoader, ModLoader}
import net.willemjan.factorio.model._

import scala.concurrent.duration.{Duration, DurationInt}

class Calculator(library: Library) {

  val assemblerMachines: Seq[AssemblingMachine] = library.assemblingMachines

/*
  private def parse() = {
    // @TODO Add FactorioDetector to find factorio installation on multiple platforms (Steam/Regular - Windows/Linux/Mac)
    val parser = new FactorioParser(
      new FactorioLoader(
        new File("C:/Program Files (x86)/Steam/steamapps/common/Factorio/data"),
        new ModLoader(new File("C:/Users/wjzijderveld/AppData/Roaming/Factorio/mods"))
      )
    )

    parser.parse()
  }
*/
  def calculate(name: String, count: Double): Unit = calculate(name, count, Duration(1.0, TimeUnit.MINUTES))
  def calculate(name: String, count: Double, duration: Duration): Unit = {
  }

  def calculateForRecipe(item: Item, recipe: Recipe, count: Double, duration: Duration, assemblingMachine: String): (Double, Int) = {

    val result = recipe.results.find(result => result.name == item.name).get
    val machine = assemblerMachines.find(m => m.name == assemblingMachine).get

    val requiredAseemblers = result.amount / duration.toSeconds * (recipe.requiredEnergy / machine.speed) * count

    (requiredAseemblers, 0)
  }

  private def makeBuild(count: Double, duration: Duration, recipe: Recipe, assemblingMachine: AssemblingMachine) = {
    val countPerSecond = count / duration.toSeconds

    println(f"${countPerSecond * recipe.requiredEnergy / assemblingMachine.speed}%1.2f x ${assemblingMachine.name}")
  }
}

object Calculator {
  def apply() = {

    lazy val library: Library = {
      val parser = new FactorioParser(
        new FactorioLoader(
          new File("C:/Program Files (x86)/Steam/steamapps/common/Factorio/data"),
          new ModLoader(new File("C:/Users/wjzijderveld/AppData/Roaming/Factorio/mods"))
        )
      )

      parser.parse()
    }

    new Calculator(library)
  }
}
