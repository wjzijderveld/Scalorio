package net.willemjan.factorio.loader
import java.io.{File, FileInputStream, IOException, InputStream}
import java.util.zip.ZipFile

import org.luaj.vm2._
import org.luaj.vm2.lib.jse.JsePlatform
import org.luaj.vm2.lib.{BaseLib, OneArgFunction, PackageLib, ResourceFinder}

import scala.io.Source

class FactorioLoader(rootDirectory: File, modLoader: ModLoader) extends LuaLoader with ResourceFinder {
  val DebugEnabled = true

  def basePath: String = rootDirectory + "/base"

  val dataloaderFile: String = rootDirectory + "/core/lualib/dataloader.lua"
  val autoplaceUtilsFile: String = rootDirectory + "/core/lualib/autoplace_utils.lua"
  val utilFile: String = rootDirectory + "/core/lualib/util.lua"

  BaseLib.FINDER = this

  val globals: LuaValue = if(DebugEnabled) JsePlatform.debugGlobals() else JsePlatform.standardGlobals()

  if (! rootDirectory.isDirectory || ! rootDirectory.canRead) {
    throw new RuntimeException(s"Unable to read $rootDirectory")
  }

  private def replaceModuleDots(module: String, replacement: String) = module.replaceFirst("module\\(\\.\\.\\.,", s"""module("$replacement",""")

  private def extractZipFilename(filename: String): (String, String) = {
    val till = filename.indexOf(".zip")

    if (till == -1) return null

    val parts = filename.splitAt(till + 4)
    (parts._1, parts._2.splitAt(1)._2)
  }

  private def appendLuaPath(path: String): Unit = {
    PackageLib.instance.setLuaPath(
      PackageLib.instance.PACKAGE.get(LuaValue.valueOf("path")).checkstring().toString + ";" +
      path
    )
  }

  private def loadLuaLib(name: String) = {
    val basePath: String = rootDirectory + "/core/lualib/"
    PackageLib.instance.setIsLoaded(name, globals.get("dofile").call(LuaValue.valueOf(s"$basePath/${name}.lua")).checktable())
  }

  override def load(filename: String): LuaTable = {
    PackageLib.instance.setLuaPath(s"${rootDirectory.getAbsolutePath}\\base\\?.lua;${rootDirectory.getAbsolutePath}\\core\\?.lua")

    val utilSource: Source = Source.fromFile(utilFile)
    val utilContent: String = try { replaceModuleDots(utilSource.mkString, "util") } finally { utilSource.close()}

    globals.get("dofile").call(LuaValue.valueOf(dataloaderFile))
    val util: LuaValue = globals.get("loadstring").call(LuaValue.valueOf(utilContent))
    util.invoke()

    loadLuaLib("autoplace_utils")
    loadLuaLib("math3d")

    globals.get("dofile").call(LuaValue.valueOf(s"$basePath\\data.lua"))
    globals.set("log", new LogFunction())

    if (DebugEnabled) println("Loaded base game data")

    def loadDataFile(mod: Mod, file: String): Unit = {
      try {
        globals.get("dofile").call(LuaValue.valueOf(s"${mod.basePath}/${mod.rawName}/$file"))
      } catch {
        case e: LuaError =>
          if (! e.getMessage.contains("No such file or directory") && DebugEnabled)
            println(s"[${mod.name}] error while loading $file: ${e.getMessage}")
        case e: Throwable => println(e.getClass, e.getMessage)
      }
    }

    val mods = modLoader.build()

    mods.foreach { mod =>
      appendLuaPath(s"${mod.basePath}/${mod.rawName}/?.lua")
      loadDataFile(mod, "data.lua")
    }
    mods.foreach { mod =>
      loadDataFile(mod, "data-updates.lua")
    }
    mods.foreach { mod =>
      loadDataFile(mod, "data-final-fixes.lua")
    }

    val data: LuaTable = globals.get("data").checktable()

    data
  }

  override def findResource(filename: String): InputStream = {
    val zipfile = extractZipFilename(filename)

    if (filename.contains("recipe-functions")) println(filename)

    if (zipfile != null) {
      val zip = new ZipFile(zipfile._1)
      val entry = zip.getEntry(zipfile._2)

      // return null when not found, it means we should try another path
      return if (entry != null) zip.getInputStream(entry) else null
    }

    val f = new File(filename)

    if ( ! f.exists() ) getClass.getResourceAsStream(if (filename.startsWith("/")) filename else s"/$filename")

    try {
      new FileInputStream(f)
    } catch {
      case e: IOException => null
    }
  }
}

private class LogFunction extends OneArgFunction {
  override final def call(msg: LuaValue): LuaValue = {
    println("[LOG] " + msg.toString)

    LuaValue.NIL
  }
}