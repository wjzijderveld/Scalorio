package net.willemjan.factorio.loader

import java.io.File
import org.luaj.vm2.LuaTable
import org.json4s._
import org.json4s.native.JsonMethods._

class ModLoader(modRoot: File, includeDisabled: Boolean = false) {

  if (! modRoot.isDirectory || ! modRoot.canRead) {
    throw new RuntimeException(s"Unable to use $modRoot as mod directory, it either is not a directory or is unreadable")
  }

  /**
    * Loads the mod-list.json and builds a list of mods in order
    * NOTE This assumes the order of mod-list is correct for dependencies
    *
    * @return
    */
  def build(): Seq[Mod] = {

    val modListFile = modRoot.listFiles((file: File) => file.getName == "mod-list.json" )

    if (modListFile.size != 1) Seq.empty

    val json = parse(FileInput(modListFile(0)))

    for {
      JObject(child) <- json
      JField("mods", JArray(mods)) <- child
      JObject(mod) <- mods
      JField("name", JString(name)) <- mod
      if name != "base"
      JField("enabled", JString(enabled)) <- mod
      if enabled == "true" || includeDisabled
    } yield buildMod(name, enabled == "true")
  }

  private def buildMod(name: String, enabled: Boolean): Mod = {
    val applicableMods = modRoot.listFiles(file => file.getName.startsWith(name + "_"))

    val directories = applicableMods.filter(file => file.isDirectory)
    val zipFiles = applicableMods.filter(file => file.getName.endsWith(".zip"))

    val modFile = if (directories.length > 0)
      directories.sorted(Ordering[File].reverse).head
    else if (zipFiles.length > 0)
      zipFiles.sorted(Ordering[File].reverse).head
    else
      throw new Exception(s"Unable to find referenced mod $name")

    val extensionLength = if (modFile.getName.contains(".zip")) 4 else 0

    Mod(
      name,
      modFile.getName.substring(0, modFile.getName.length - extensionLength),
      modFile.getAbsolutePath
    )
  }
}

case class Mod(name: String, rawName: String, basePath: String)
