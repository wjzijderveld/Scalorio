package net.willemjan.factorio.model

case class Library(
  items: Seq[Item],
  fluids: Seq[Fluid],
  recipes: Seq[Recipe],
  assemblingMachines: Seq[AssemblingMachine],
  rocketSilo: RocketSilo
)

object Library {
  def empty = new Library(Seq.empty, Seq.empty, Seq.empty, Seq.empty, RocketSilo())
}

object IngredientType extends Enumeration {
  type Type = Value
  val Fluid, Item = Value
}

object RecipeCategory extends Enumeration {
  type Category = String
  // Base game
  val Crafting = "crafting"
  val AdvancedCrafting = "advanced-crafting"
  val CraftingWithFluid = "crafting-with-fluids"
  val OilProcessing = "oil-processing"
  val Chemistry = "chemistry"
  val Centrifuging = "centrifuging"
  val Smelting = "smelting"
  val RocketBuilding = "rocket-building"
  // Mods
  val WaterPump = "water-pump"
}
import IngredientType._
import RecipeCategory._

abstract class AbstractItem

case class Item(name: String, subGroup: String, icon: String, placeResult: String, stackSize: Int, craftingCategories: Seq[String] = Seq.empty) extends AbstractItem
case class Fluid(name: String) extends AbstractItem
case class Recipe(name: String, category: Category, ingredients: Seq[Ingredient], requiredEnergy: Double, results: Seq[RecipeResult]) extends AbstractItem {
  def withIngredients(ingredients: Seq[Ingredient]): Recipe = this.copy(ingredients = ingredients)
}
case class AssemblingMachine(name: String, speed: Double, ingredientCount: Int, categories: Seq[Category]) extends AbstractItem
case class Ingredient(name: String, itemType: Type, amount: Double, var icon: Option[String]) extends AbstractItem {
  def withIcon(icon: Option[String]): Ingredient = this.copy(icon = icon)
}
case class RocketSilo() extends AbstractItem
case class Furnace() extends AbstractItem
case class UnmappedItem() extends AbstractItem

case class RecipeResult(name: String, amount: Double, probability: Double, itemType: String)
