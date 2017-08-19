package net.willemjan.factorio.gui.recipe

import java.io.File
import javax.swing.ImageIcon
import javax.swing.border.EmptyBorder

import net.willemjan.factorio.calculator.Calculator
import net.willemjan.factorio.gui.RecipeSearch
import net.willemjan.factorio.gui.recipe.calculations.event._
import net.willemjan.factorio.gui.recipe.calculations._
import net.willemjan.factorio.model._

import scala.concurrent.duration.TimeUnit
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.Swing.EmptyIcon
import scala.swing.TabbedPane.Page
import scala.swing._
import scala.swing.event._

class RecipesPane(val search: RecipeSearch, val library: Library, val calculator: Calculator) extends TabbedPane {
  def setRecipes(forItem: Item, recipes: Seq[Recipe]): Unit = {
    peer.removeAll()

    recipes.foreach(recipe => {
      pages += new Page(recipe.name, new RecipePane(forItem, recipe, this))
    })

  }

  def setAmountAndTimeUnit(amount: Int, timeUnit: TimeUnit, recipe: Option[Recipe] = None): Unit = recipe match {
    case None => pages(0).content.asInstanceOf[RecipePane].setAmountAndTimeUnit(amount, timeUnit)
    case Some(recipe) => pages.find(p => p.title == recipe.name).get.asInstanceOf[RecipePane].setAmountAndTimeUnit(amount, timeUnit)
  }
}

class RecipePane(forItem: Item, recipe: Recipe, parent: RecipesPane) extends BorderPanel {
  implicit val calculator: Calculator = parent.calculator
  val library = parent.library

  border = new EmptyBorder(10, 10, 10, 10)

  lazy val items: Seq[Item] = library.items
  val basePath = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\Factorio\\data\\base" // move to some sort of config object

  layout(new GridBagPanel {
    val c = new Constraints
    c.weightx = 1.0
    c.weighty = 0.0
    c.gridwidth = 1
    c.anchor = Anchor.NorthEast
    c.fill = Fill.Horizontal

    c.gridx = 0
    c.gridy = 0
    layout(new Label("Name", EmptyIcon, Alignment.Left)) = c
    c.gridy = 1
    layout(new Label("Category", EmptyIcon, Alignment.Left)) = c
    c.gridy = 2
    layout(new Label("Required energy", EmptyIcon, Alignment.Left)) = c

    c.gridx = 1
    c.gridy = 0
    layout(new Label(recipe.name, EmptyIcon, Alignment.Left)) = c
    c.gridy = 1
    layout(new Label(recipe.category, EmptyIcon, Alignment.Left)) = c
    c.gridy = 2
    layout(new Label(recipe.requiredEnergy.toString, EmptyIcon, Alignment.Left)) = c


    c.gridheight = 3
    c.gridx = 3
    c.gridy = 0
    c.ipadx = 5
    layout(new GridBagPanel {
      val c = new Constraints
      c.weightx = 1.0
      c.weighty = 1.0
      c.gridwidth = 1
      c.anchor = Anchor.NorthEast
      c.fill = Fill.Horizontal

      recipe.results.indices.foreach { index => {
        c.gridx = 0 + index
        c.gridy = 0
        layout(new Label {
          val result: RecipeResult = recipe.results(index)
          text = result.probability match {
            case 1.0 => s"${result.amount.toString}"
            case _ => f"${result.amount.toString} (${result.probability * 100}%2.2f%%)"
          }
          val item: Option[Item] = if (result.itemType == "item") items.find(item => item.name == result.name) else None
          icon = item match {
            case Some(x) =>
              val uri = new File(x.icon.replace("__base__", basePath)).toURI
              new ImageIcon(uri.toURL)
            case None => new ImageIcon(basePath + "\\graphics\\achievement\\so-long-and-thanks-for-all-the-fish.png")
          }
          verticalTextPosition = Alignment.Bottom
          horizontalTextPosition = Alignment.Center
        }) = c
      }}

    }) = c
  }) = BorderPanel.Position.North

  import RecipeCategory._

  val recipeCalculationPanel: CalculationPanel = recipe.category match {
    case Crafting | CraftingWithFluid | AdvancedCrafting | OilProcessing | Chemistry | Centrifuging => new CraftingCalculation(parent.search, forItem, recipe, library.assemblingMachines)
    case Smelting => ???
    case RocketBuilding => new RocketSiloCalculation(parent.search, forItem, recipe)
  }

  listenTo(recipeCalculationPanel)
  layout(recipeCalculationPanel) = BorderPanel.Position.Center

  def setAmountAndTimeUnit(amount: Int, timeUnit: TimeUnit): Unit = {
    recipeCalculationPanel.setAmountAndTimeUnit(amount, timeUnit)
  }

}
