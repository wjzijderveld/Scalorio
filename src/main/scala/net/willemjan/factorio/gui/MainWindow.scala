package net.willemjan.factorio.gui

import java.io.File

import net.willemjan.factorio.calculator.{Calculator, FactorioParser}
import net.willemjan.factorio.loader.{FactorioLoader, ModLoader}
import net.willemjan.factorio.model.Library

import scala.swing._

object MainWindow extends SimpleSwingApplication {

  lazy val library: Library = {
    val parser = new FactorioParser(
      new FactorioLoader(
        new File("C:/Program Files (x86)/Steam/steamapps/common/Factorio/data"),
        new ModLoader(new File("C:/Users/wjzijderveld/AppData/Roaming/Factorio/mods"))
      )
    )

    parser.parse()
  }

  lazy val calculator: Calculator = new Calculator(library)
  lazy val search: RecipeSearch = new RecipeSearch(library, calculator)

  def top = new MainFrame {
    val minimumDimension = new Dimension(800,600)
    title = "The adapthing Factorio Calculator"
    contents = search
    minimumSize = minimumDimension
    preferredSize = minimumDimension
  }
}
