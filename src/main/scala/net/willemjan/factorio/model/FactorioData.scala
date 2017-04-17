package net.willemjan.factorio.model

class Data( val items: Map[String, Item],
            val recipes: Map[String, Recipe],
            val assemblingMachines: Map[String, AssemblingMachine]) {

}

object Data {
  def apply(items: Map[String, Item], recipes: Map[String, Recipe], assemblingMachines: Map[String, AssemblingMachine]) = new Data(items, recipes, assemblingMachines)
  def empty = new Data(Map.empty, Map.empty, Map.empty)
}

object Type extends Enumeration {
  type Type = Value
  val Fluid, Item = Value
}

object AssemblingCategory extends Enumeration {
  type Category = Value
  val
    Crafting,
    AdvancedCrafting,
    OilProcessing,
    Chemistry,
    Smelting,
    RocketBuilding,
    CraftingWithFluid = Value
}
import Type._
import AssemblingCategory._

abstract class AbstractItem

case class Item(name: String, icon: String, placeResult: String, stackSize: Int) extends AbstractItem
case class Recipe(name: String, category: Category, ingredients: Seq[Ingredient], requiredEnergy: Integer, results: Seq[RecipeResult]) extends AbstractItem
case class AssemblingMachine(name: String, speed: Double, ingredientCount: Int, categories: Seq[Category]) extends AbstractItem
case class Ingredient(name: String, itemType: Type, amount: Double) extends AbstractItem
case class UnmappedItem() extends AbstractItem

case class RecipeResult(name: String, amount: Double, itemType: String)
