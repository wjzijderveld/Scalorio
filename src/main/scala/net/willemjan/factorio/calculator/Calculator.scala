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

  def calculateRocketBuilding(item: Item, recipe: Recipe, amount: Int, duration: Duration, modules: Modules = Modules.empty): CalculationResult = {
    val amountPerCraft = recipe.results.find(result => result.name == item.name).get.amount
    val timePerCraft = recipe.requiredEnergy

    val craftCount = calculateRequiredCrafters(amount, duration, amountPerCraft, timePerCraft, modules)

    CalculationResult(craftCount, 0.0, recipe.ingredients.map(ingredient => {
      ingredient.copy(amount = ingredient.amount / modules.productivity.getModifier * amount)
    }))
  }

  def calculateCrafting(item: Item, recipe: Recipe, assembler: AssemblingMachine, amount: Int, duration: Duration, modules: Modules = Modules.empty): CalculationResult = {
    val amountPerCraft = recipe.results.find(result => result.name == item.name).get.amount
    val timePerCraft = recipe.requiredEnergy * assembler.speed

    val craftCount = calculateRequiredCrafters(amount, duration, amountPerCraft, timePerCraft, modules)

    CalculationResult(craftCount, 0.0, recipe.ingredients.map(ingredient => {
      ingredient.copy(amount = ingredient.amount / modules.productivity.getModifier * amount)
    }))
  }

  def calculateRequiredCrafters(amount: Int, duration: Duration, amountPerCraft: Double, timePerCraft: Double, modules: Modules): Int = {
    val itemsPerSecond: Double = amount.toDouble / duration.toSeconds
    val craftResultsPerSecond = (amountPerCraft.toDouble / modules.productivity.getModifier) / (timePerCraft.toDouble / modules.speed.getModifier)
    val value = Math.round(itemsPerSecond / craftResultsPerSecond * 100)
    println(s"$itemsPerSecond / ( $amountPerCraft / ${modules.productivity.getModifier} ) / ( $timePerCraft / ${modules.speed.getModifier} ) = ${value / 100}")
    Math.ceil(value / 100).toInt
  }

}

// Module.speed(3)(4)

case class CalculationResult(crafters: Int, energyRequired: Double, ingredients: Seq[Ingredient])
case class Modules(speed: Module, productivity: Module)
case class Module(count: Int, levelBonus: Double) {
  def getModifier: Double = 1 + (count * levelBonus)
}
object Module {
  val Speed: Seq[Double] = Seq(0.2, 0.3, 0.5)
  val Productivity: Seq[Double] = Seq(0.04, 0.06, 0.1)

  def empty = Module(0, 0.0)
  def speed(level: Int)(count: Int) = Module(count, Speed(level - 1))
  def productivity(level: Int)(count: Int) = Module(count, Productivity(level - 1))
}
object Modules {
  def empty = Modules(Module.empty, Module.empty)
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
