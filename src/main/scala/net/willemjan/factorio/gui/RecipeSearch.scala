package net.willemjan.factorio.gui

import net.willemjan.factorio.calculator.Calculator
import net.willemjan.factorio.gui.recipe.RecipesPane
import net.willemjan.factorio.gui.swing.CardPanel
import net.willemjan.factorio.model.Library

import scala.concurrent.duration.TimeUnit
import scala.swing._
import scala.swing.event._

class RecipeSearch(library: Library, calculator: Calculator) extends BorderPanel {

  final val PlaceholderPane = "placeholder"
  final val TabbedRecipePane = "recipes"

  lazy val itemNames: Seq[String] = library.items.map(_.name).sorted

  minimumSize = new Dimension(600, 400)
  preferredSize = new Dimension(600, 400)

  object searchField extends TextField
  object listView extends ListView(itemNames)
  object optionPane extends BorderPanel {
    layout(searchField) = BorderPanel.Position.North
    layout(new ScrollPane {
      contents = listView
    }) = BorderPanel.Position.Center
  }
  object recipePane extends RecipesPane(this, library, calculator) {}
  object contentPanel extends CardPanel {
    add(new TabbedPane, PlaceholderPane)
    add(recipePane, TabbedRecipePane)
  }

  def changeItem(item: String, amount: Int, timeUnit: TimeUnit): Unit = {
    val index = itemNames.indexOf(item)
    deafTo(searchField, listView.selection)
    searchField.text = item
    listView.listData = Seq(item)
    listenTo(searchField, listView.selection)

    listView.selectIndices(0)
    recipePane.setAmountAndTimeUnit(amount, timeUnit)
  }

  listenTo(searchField, listView.selection)

  reactions += {
    case ValueChanged(`searchField`) =>
      println("searchField updated")
      listView.listData = itemNames.filter(_.contains(searchField.text)).sorted
    case SelectionChanged(`listView`) => if (! listView.selection.adjusting) {
      println("Changed list selection", listView.selection.items)
      if (listView.selection.items.nonEmpty) {
        val newItem = listView.selection.items(0)
        val recipes = library.recipes.filter(recipe => recipe.results.exists(result => result.name == newItem))

        if (recipes.isEmpty) {
          contentPanel.show(PlaceholderPane)
        } else {
          recipePane.setRecipes(library.items.find(item => item.name == newItem).get, recipes)
          contentPanel.show(TabbedRecipePane)
        }
      }

      if (listView.selection.items.isEmpty) {
        // deselect, but probably because something else got focus
      }
    }
  }

  layout(optionPane) = BorderPanel.Position.West
  layout(contentPanel) = BorderPanel.Position.Center
}
