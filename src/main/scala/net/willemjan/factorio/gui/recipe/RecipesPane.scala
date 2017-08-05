package net.willemjan.factorio.gui.recipe

import java.io.File
import javax.swing.ImageIcon
import javax.swing.border.EmptyBorder

import net.willemjan.factorio.calculator.Calculator
import net.willemjan.factorio.model._

import scala.concurrent.duration.FiniteDuration
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.Swing.EmptyIcon
import scala.swing.TabbedPane.Page
import scala.swing._
import scala.swing.event._

class RecipesPane(library: Library, calculator: Calculator) extends TabbedPane {
  def setRecipes(forItem: String, recipes: Seq[Recipe]): Unit = {
    peer.removeAll()

    recipes.foreach(recipe => {
      pages += new Page(recipe.name, new RecipePane(forItem, recipe, library, calculator))
    })

  }
}

class RecipePane(forItem: String, recipe: Recipe, library: Library, calculator: Calculator) extends GridBagPanel {
  border = new EmptyBorder(10, 10, 10, 10)

  lazy val items: Seq[Item] = library.items
  val basePath = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\Factorio\\data\\base" // move to some sort of config object

  final val InfoPane = "info"
  final val ConfigurationPane = "config"

  val c = new Constraints
  c.weightx = 1.0
  c.weighty = 1.0
  c.gridwidth = 1
  c.anchor = Anchor.North
  c.fill = Fill.Both

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
  }) = c


  /**
    * @todo Make a different layout for the different types of recipee
    *       - crafting
    *       - advanced-crafting
    *       - crafting-with-fluid
    *       - oil-processing
    *       - chemistry
    *       - centriuging
    *
    *       - smelting
    *
    *       - rocket-building
    */


  import RecipeCategory._

  recipe.category match {
    case Crafting | CraftingWithFluid | AdvancedCrafting | OilProcessing | Chemistry | Centrifuging => // whatever we did
    case Smelting => ???
    case RocketBuilding => ???
  }

  object assemblerCountLabel extends Label("0 assemblers")
  object amountField extends TextField("1") { columns = 4 }
  object durationField extends ComboBox(Seq("second", "minute"))
  object assemblerField extends ComboBox(library.assemblingMachines.map(m => m.name).sorted)

  def recalculate(): Unit = {
    val result = calculator.calculateForRecipe(
      items.find(item => item.name == forItem).get,
      recipe,
      amountField.text.toDouble,
      FiniteDuration(1L, durationField.selection.item),
      assemblerField.selection.item
    )

    assemblerCountLabel.text = f"${result._1}%2.2f assemblers"
  }

  reactions += {
    case ValueChanged(`amountField`) => recalculate()
  }

  listenTo(amountField, durationField.selection, assemblerField.selection)

  c.gridy = 1
  layout(new FlowPanel {
    contents.append(
      amountField,
      new Label("item(s) per", EmptyIcon, Alignment.Left),
      durationField,
      new Label("using"),
      assemblerField,
      new Label("assemblers with"),
      new TextField("0"),
      new Label("speed modules and"),
      new TextField("0"),
      new Label("productivity modules")
    )
  }) = c

  c.gridy = 2
  layout(new GridBagPanel {

    recalculate()

    val c = new Constraints
    c.weightx = 1.0
    c.weighty = 1.0
    c.gridwidth = 1
    c.anchor = Anchor.North
    c.fill = Fill.Both

    c.gridx = 0
    c.gridy = 0
    layout(assemblerCountLabel) = c
    c.gridy = 1
    layout(new Label("# kJ")) = c

    c.gridx = 1
    c.gridy = 0
    recipe.ingredients.indices.foreach { index => {
      c.gridy = 0 + index
      val ingredient = recipe.ingredients(index)
      val icon = items.find(item => item.name == ingredient.name) match {
        case Some(item) =>
          val uri = new File(item.icon.replace("__base__", basePath)).toURI
          new ImageIcon(uri.toURL)
        case _ => EmptyIcon
      }
      layout(new Label(s"${ingredient.amount} x ${ingredient.name}", icon, Alignment.Left)) = c
    }}

  }) = c

  // val ingredientTable = Table(recipe.ingredients.map(ingredient => Array(ingredient.name, ingredient.amount)).toArray, Seq("Ingredient", "Amount")) {
  // }
}
