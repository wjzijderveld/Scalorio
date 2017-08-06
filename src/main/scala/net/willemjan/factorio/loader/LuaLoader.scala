package net.willemjan.factorio.loader

import org.luaj.vm2.LuaTable

trait LuaLoader {
  def load(filename: String): LuaTable
  def basePath: String
}
