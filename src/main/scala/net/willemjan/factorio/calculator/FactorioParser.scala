package net.willemjan.factorio.calculator

import java.io.{File, FileInputStream, IOException, InputStream}
import java.util.zip.ZipFile

import net.willemjan.factorio.model.AssemblingCategory._
import net.willemjan.factorio.model._
import org.luaj.vm2.{LuaString, LuaTable, LuaValue, Varargs}
import org.luaj.vm2.lib.{BaseLib, PackageLib, ResourceFinder}
import org.luaj.vm2.lib.jse.JsePlatform

import scala.io.Source

class FactorioParser(gameRoot: File, modsRoot: File) {

  val DEFAULT_STACKSIZE = 50
  val debugEnabled = true

  if (! gameRoot.isDirectory || ! gameRoot.canRead) {
    throw new RuntimeException(s"Unable to read $gameRoot")
  }

  private def replaceModuleDots(string: String) = string.replaceFirst("module\\(\\.\\.\\.,", "module(\"util\",")

  val dataloaderFile: String = gameRoot + "/core/lualib/dataloader.lua"
  val utilFile: String = gameRoot + "/core/lualib/util.lua"
  val autoplaceUtilsFile: String = gameRoot + "/core/lualib/autoplace_utils.lua"
  val utilSource: Source = Source.fromFile(utilFile)
  val utilContent: String = try { replaceModuleDots(utilSource.mkString) } finally { utilSource.close()}

  private def extractZipFilename(filename: String): (String, String) = {
    val till = filename.indexOf(".zip")

    if (till == -1) return null

    val parts = filename.splitAt(till + 4)
    (parts._1, parts._2.splitAt(1)._2)
  }


  BaseLib.FINDER = new ResourceFinder {
    override def findResource(filename: String): InputStream = {
      val zipfile = extractZipFilename(filename)

      if (zipfile != null) {
        val zip = new ZipFile(zipfile._1)
        val entry = zip.getEntry(zipfile._2)

        println(entry)

        return zip.getInputStream(entry)
      }

      val f = new File(filename)
      if ( ! f.exists() )
        getClass.getResourceAsStream(if (filename.startsWith("/")) filename else s"/$filename")
      try {
        return new FileInputStream(f)
      } catch {
        case e: IOException =>
          return null
      }
    }
  }

  val globals: LuaValue = if(debugEnabled) JsePlatform.debugGlobals() else JsePlatform.standardGlobals()
  PackageLib.instance.setLuaPath(s"${gameRoot.getAbsolutePath}\\base\\?.lua;${gameRoot.getAbsolutePath}\\core\\?.lua;${modsRoot.getAbsolutePath}/bobores_0.14.0.zip/bobores_0.14.0/?.lua")

  private def loadLuaLib(name: String) = {
    val basePath: String = gameRoot + "/core/lualib/"
    PackageLib.instance.setIsLoaded(name, globals.get("dofile").call(LuaValue.valueOf(s"$basePath/${name}.lua")).checktable())
  }

  globals.get("dofile").call(LuaValue.valueOf(dataloaderFile))
  val util: LuaValue = globals.get("loadstring").call(LuaValue.valueOf(utilContent))
  util.invoke()

  loadLuaLib("autoplace_utils")
  loadLuaLib("math3d")

  globals.get("dofile").call(LuaValue.valueOf(gameRoot + "\\base\\data.lua"))

  val data: LuaTable = globals.get("data").checktable()

  def parse(): Data = {
    val parsedData = loadModData(build(data.get("raw").checktable()), modsRoot)

    Data(
      parsedData.getOrElse("item", Map.empty).asInstanceOf[Map[String, Item]],
      parsedData.getOrElse("recipe", Map.empty).asInstanceOf[Map[String, Recipe]],
      parsedData.getOrElse("assembling-machine", Map.empty).asInstanceOf[Map[String, AssemblingMachine]]
    )
  }

  private def loadModData(baseData: RawData, modPath: File): RawData = {
    globals.get("dofile").call(LuaValue.valueOf("C:/Users/wjzijderveld/AppData/Roaming/Factorio/mods/bobores_0.14.0.zip/bobores_0.14.0/data.lua"))
    Map.empty
  }

  private def build(typedTable: LuaTable): RawData = {
    def walk(typedTable: LuaTable, from: LuaValue, acc: RawData): RawData = typedTable.next(from) match {
      case LuaValue.NIL => acc
      case args: Varargs => walk(typedTable, args.arg1(), acc + (args.arg1().tojstring() -> buildItems(args.arg(2).checktable())))
    }

    walk(typedTable, LuaValue.NIL, Map.empty)
  }

  private def buildItems(itemTable: LuaTable): Map[String, AbstractItem] = {
    def walk(itemTable: LuaTable, from: LuaValue, acc: Map[String, AbstractItem]): Map[String, AbstractItem] = itemTable.next(from) match {
      case LuaValue.NIL => acc
      case args: Varargs => walk(itemTable, args.arg1(), acc + (args.arg1().tojstring() -> buildItem(args.arg(2))))
    }

    walk(itemTable, LuaValue.NIL, Map.empty)
  }

  private def buildItem(luaItem: LuaValue): AbstractItem = luaItem match {
    case LuaValue.NIL => throw new Exception("Found NIL item, no way to handle this")
    case table: LuaTable =>
      table.get("type").toString match {
        case "item" => Item(table.get("name").toString, table.get("icon").toString, table.get("place_result").toString, table.get("stack_size").optint(DEFAULT_STACKSIZE))
        case "recipe" => Recipe(table.get("name").toString, mapCraftingCategory(table.get("category").optstring(LuaString.valueOf("crafting")).toString), Seq.empty, table.get("energy_required").optint(1), buildResults(table))
        case "assembling-machine" => AssemblingMachine(
          table.get("name").toString,
          table.get("crafting_speed").todouble(),
          table.get("ingredient_count").toint(),
          buildCategories(table.get("crafting_categories").checktable())
        )
        case _ => UnmappedItem()
      }
  }

  private def buildResults(table: LuaTable): Seq[RecipeResult] = table.get("result") match {
    case LuaValue.NIL => table.get("results") match {
      case LuaValue.NIL => Seq.empty
      case subTable: LuaTable =>
        def walk(table: LuaTable, from: LuaValue, acc: Seq[RecipeResult]): Seq[RecipeResult] = table.next(from) match {
          case LuaValue.NIL => acc
          case args: Varargs => walk(table, args.arg1(), acc :+ buildRecipeResult(args.arg(2).checktable()))
        }
        walk(subTable, LuaValue.NIL, Seq.empty)
    }
    case value: LuaValue if value.isstring() => Seq(RecipeResult(value.toString, 1.0, "item"))
  }

  private def buildRecipeResult(table: LuaTable): RecipeResult = {
    RecipeResult(table.get("name").toString, table.get("amount").optdouble(1.0), table.get("type").toString)
  }

  private def buildCategories(table: LuaTable): Seq[Category] = {
    def walk(table: LuaTable, from: LuaValue, acc: Seq[Category]): Seq[Category] = table.next(from) match {
      case LuaValue.NIL => acc
      case args: Varargs => walk(table, args.arg1(), acc :+ mapCraftingCategory(args.arg(2).toString))
    }

    walk(table, LuaValue.NIL, Seq.empty)
  }

  private def mapCraftingCategory(category: String): Category = category match {
        case "oil-processing" => OilProcessing
        case "crafting" => Crafting
        case "advanced-crafting" => AdvancedCrafting
        case "chemistry" => Chemistry
        case "crafting-with-fluid" => CraftingWithFluid
        case "smelting" => Smelting
        case "rocket-building" => RocketBuilding
        case "nil" => throw new Exception("NIL value found for category")
        case some: String => throw new Exception(s"Unknown category $some found")
      }
}
