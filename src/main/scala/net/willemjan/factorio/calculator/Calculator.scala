package net.willemjan.factorio.calculator

import java.io.File
import java.util.concurrent.TimeUnit

import net.willemjan.factorio.loader.{FactorioLoader, ModLoader}
import net.willemjan.factorio.model._

import scala.concurrent.duration.{Duration, DurationInt}

class Calculator(library: Library) {

  val assemblerMachines: Seq[AssemblingMachine] = library.assemblingMachines.values.toSeq

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
    val data = library

    // data.items.map(item => item._2.subGroup).toSeq.distinct.foreach { println }

    println(data.recipes.get(name))
    println(data.fluids.get(name))

    /**
      * 1. get recipes with result $name - return ordered by cheapest? - later extendable by available research
      * 2.
      */

    data.recipes.get(name) match {
      case Some(recipe) => data.assemblingMachines.filter { item => item._2.categories.contains(recipe.category)} foreach { machine => makeBuild(count, duration, recipe, machine._2) }
      case None => println(s"Unable to find specific recipe for $name")
    }

    val recipesWithResult = data.recipes.filter(r => r._2.results.exists(result => result.name == name))
    recipesWithResult.foreach { println }
  }

  def calculateForRecipe(item: Item, recipe: Recipe, count: Double, duration: Duration, assemblingMachine: String): (Double, Int) = {

    println(library.items.values.filter(_.craftingCategories.contains(recipe.category)))
    println(library.items.values.map(x => x.craftingCategories).toSeq.flatten.distinct)

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
