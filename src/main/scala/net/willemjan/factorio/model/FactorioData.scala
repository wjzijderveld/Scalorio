package net.willemjan.factorio.model

class Library(val items: Map[String, Item],
              val fluids: Map[String, Fluid],
              val recipes: Map[String, Recipe],
              val assemblingMachines: Map[String, AssemblingMachine]) {

}

object Library {
  def apply(items: Map[String, Item], fluids: Map[String, Fluid], recipes: Map[String, Recipe], assemblingMachines: Map[String, AssemblingMachine]) = new Library(items, fluids, recipes, assemblingMachines)
  def empty = new Library(Map.empty, Map.empty, Map.empty, Map.empty)
}

object IngredientType extends Enumeration {
  type Type = Value
  val Fluid, Item = Value
}

object AssemblingCategory extends Enumeration {
  type Category = String
  val
    // Base Game
    Crafting,
    AdvancedCrafting,
    OilProcessing,
    Chemistry,
    Smelting,
    RocketBuilding,
    CraftingWithFluid,
    // Mods
    WaterPump
  = Value
}
import IngredientType._
import AssemblingCategory._

abstract class AbstractItem

case class Item(name: String, subGroup: String, icon: String, placeResult: String, stackSize: Int, craftingCategories: Seq[String] = Seq.empty) extends AbstractItem
case class Fluid(name: String) extends AbstractItem
case class Recipe(name: String, category: Category, ingredients: Seq[Ingredient], requiredEnergy: Integer, results: Seq[RecipeResult]) extends AbstractItem
case class AssemblingMachine(name: String, speed: Double, ingredientCount: Int, categories: Seq[Category]) extends AbstractItem
case class Ingredient(name: String, itemType: Type, amount: Double) extends AbstractItem
case class UnmappedItem() extends AbstractItem

case class RecipeResult(name: String, amount: Double, probability: Double, itemType: String)
