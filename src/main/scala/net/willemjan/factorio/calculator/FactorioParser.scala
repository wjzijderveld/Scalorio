package net.willemjan.factorio.calculator

import net.willemjan.factorio.loader.LuaLoader
import net.willemjan.factorio.model.RecipeCategory._
import net.willemjan.factorio.model._
import org.luaj.vm2.{LuaString, LuaTable, LuaValue, Varargs}

class FactorioParser(loader: LuaLoader) {

  final val DefaultAmount = 1.0
  final val DefaultProbability = 1.0

  val DefaultStacksize = 50
  val data: LuaTable = loader.load("data.lua")

  def parse(): Library = {
    val parsedData = build(data.get("raw").checktable())


    Library(
      parsedData.filter(_.isInstanceOf[Item]).asInstanceOf[Seq[Item]],
      parsedData.filter(_.isInstanceOf[Fluid]).asInstanceOf[Seq[Fluid]],
      parsedData.filter(_.isInstanceOf[Recipe]).asInstanceOf[Seq[Recipe]],
      parsedData.filter(_.isInstanceOf[AssemblingMachine]).asInstanceOf[Seq[AssemblingMachine]],
      parsedData.find(_.isInstanceOf[RocketSilo]).getOrElse(RocketSilo()).asInstanceOf[RocketSilo]
    )
  }

  private def build(typedTable: LuaTable): Seq[AbstractItem] = {
    def walk(typedTable: LuaTable, from: LuaValue, acc: Seq[AbstractItem]): Seq[AbstractItem] = typedTable.next(from) match {
      case LuaValue.NIL => acc
      case args: Varargs => walk(typedTable, args.arg1(), acc ++ buildItems(args.arg(2).checktable()))
    }

    walk(typedTable, LuaValue.NIL, Seq.empty)
  }

  private def buildItems(itemTable: LuaTable): Seq[AbstractItem] = {
    def walk(itemTable: LuaTable, from: LuaValue, acc: Seq[AbstractItem]): Seq[AbstractItem] = itemTable.next(from) match {
      case LuaValue.NIL => acc
      case args: Varargs => walk(itemTable, args.arg1(), acc :+ buildItem(args.arg(2)))
    }

    walk(itemTable, LuaValue.NIL, Seq.empty)
  }

  private def getTableValues(table: LuaTable, from: LuaValue, acc: Seq[String]): Seq[String] = table.next(from) match {
    case LuaValue.NIL => acc
    case args: Varargs => getTableValues(
      table,
      args.arg1(),
      acc :+ args.arg1().checkjstring()
    )
  }

  private def buildItem(luaItem: LuaValue): AbstractItem = luaItem match {
    case LuaValue.NIL => throw new Exception("Found NIL item, no way to handle this")
    case table: LuaTable =>
      table.get("type").toString match {
        case "item" => Item(
          table.get("name").tojstring(),
          table.get("subgroup").tojstring(),
          table.get("icon").tojstring(),
          table.get("place_result").tojstring(),
          table.get("stack_size").optint(DefaultStacksize),
          table.get("crafting_categories") match {
            case LuaValue.NIL => Seq.empty
            case table: LuaTable => getTableValues(table, LuaValue.NIL, Seq.empty)
          }
        )
        case "fluid" => Fluid(table.get("name").toString)
        case "recipe" => Recipe(
          table.get("name").toString,
          mapCraftingCategory(table.get("category").optstring(LuaString.valueOf("crafting")).toString),
          buildIngredients(table),
          table.get("energy_required").optint(1),
          if (table.keys().exists(key => Seq("normal", "expensive").contains(key.tojstring()))) {
            buildResults(table.get("normal").checktable())
          } else {
            buildResults(table)
          }
        )
        case "assembling-machine" => AssemblingMachine(
          table.get("name").toString,
          table.get("crafting_speed").todouble(),
          table.get("ingredient_count").toint(),
          buildCategories(table.get("crafting_categories").checktable())
        )
        case "furnace" => Furnace()
        case "rocket-silo" => RocketSilo()
        case _ => UnmappedItem()
      }
  }

  private def buildIngredients(table: LuaTable): Seq[Ingredient] = table.get("ingredients") match {
    case LuaValue.NIL => Seq.empty
    case table: LuaTable =>
      def walk(table: LuaTable, from: LuaValue, acc: Seq[Ingredient]): Seq[Ingredient] = table.next(from) match {
        case LuaValue.NIL => acc
        case args: Varargs => walk(table, args.arg1(), acc :+ buildIngredient(args.arg(2)))
      }

      walk(table, LuaValue.NIL, Seq.empty)
  }

  private def buildIngredient(value: LuaValue): Ingredient = value match {
    case LuaValue.NIL => null
    case table: LuaTable =>
      // Figure out how to determine arguments
      // {1=name, 2=amount}
      // {amount=, name=, type=}
      if (table.keys().head.isint()) {
        Ingredient(
          table.get(1).checkstring().toString,
          IngredientType.Item,
          table.get(2).checknumber().todouble()
        )
      } else {
        val itemType = table.get("type").checkstring().toString match {
          case "item" => IngredientType.Item
          case "fluid" => IngredientType.Fluid
          case unknown => throw new Exception(s"Unknown ingredient type $unknown found")
        }
        Ingredient(table.get("name").checkstring().toString, itemType, table.get("amount").checknumber().todouble())
      }
  }

  private def buildResults(table: LuaTable): Seq[RecipeResult] =table.get("result") match {
    case LuaValue.NIL => table.get("results") match {
      case LuaValue.NIL => Seq.empty
      case subTable: LuaTable =>
        def walk(table: LuaTable, from: LuaValue, acc: Seq[RecipeResult]): Seq[RecipeResult] = table.next(from) match {
          case LuaValue.NIL => acc
          case args: Varargs => walk(table, args.arg1(), acc :+ buildRecipeResult(args.arg(2).checktable()))
        }
        walk(subTable, LuaValue.NIL, Seq.empty)
    }
    case value: LuaValue if value.isstring() => Seq(RecipeResult(value.toString, DefaultAmount, DefaultProbability, "item"))
  }

  private def buildRecipeResult(table: LuaTable): RecipeResult = {
    RecipeResult(
      table.get("name").toString,
      table.get("amount").optdouble(DefaultAmount),
      table.get("probability").optdouble(DefaultProbability),
      table.get("type").optjstring("item")
    )
  }

  private def buildCategories(table: LuaTable): Seq[Category] = {
    def walk(table: LuaTable, from: LuaValue, acc: Seq[Category]): Seq[Category] = table.next(from) match {
      case LuaValue.NIL => acc
      case args: Varargs => walk(table, args.arg1(), acc :+ mapCraftingCategory(args.arg(2).toString))
    }

    walk(table, LuaValue.NIL, Seq.empty)
  }

  private def mapCraftingCategory(category: String): Category = category/* match {

        // Base Game
        case "oil-processing" => OilProcessing
        case "crafting" => Crafting
        case "advanced-crafting" => AdvancedCrafting
        case "chemistry" => Chemistry
        case "crafting-with-fluid" => CraftingWithFluid
        case "smelting" => Smelting
        case "rocket-building" => RocketBuilding
        // Mods
        case "water-pump" => WaterPump
        case "nil" => throw new Exception("NIL value found for category")
        case some: String => throw new Exception(s"Unknown category $some found")
      }
      */
}
