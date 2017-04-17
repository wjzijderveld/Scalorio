package net.willemjan.factorio.loader
import org.luaj.vm2.LuaTable

class ModLoader extends LuaLoader {
  override def load(filename: String): LuaTable = {

    new LuaTable()
  }

  def buildModList(): Seq[Mod] = {

    Seq.empty
  }
}

case class Mod()
