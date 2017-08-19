package net.willemjan.factorio.gui.recipe.calculations

import javax.swing.ImageIcon

import net.willemjan.factorio.gui.recipe.calculations.event.IngredientClicked
import net.willemjan.factorio.model.{Ingredient, IngredientType, Item}

import scala.swing.Swing.EmptyIcon
import scala.swing._
import scala.swing.event._

case class IngredientLabel(ingredient: Ingredient) extends Button with Publisher {
  text = f"${ingredient.amount}%2.2f x ${ingredient.name}"
  horizontalTextPosition = Alignment.Right
  horizontalAlignment = Alignment.Left
  contentAreaFilled = false
  borderPainted = false
  icon = ingredient.icon match {
    case None => EmptyIcon
    case Some(icon) => new ImageIcon(icon)
  }

  if (ingredient.itemType == IngredientType.Item) { // Ignore fluids for now
    listenTo(mouse.moves)
  }

  reactions += {
    case event: ButtonClicked =>
      println(s"Button clicked $event")
      publish(IngredientClicked(ingredient.name, Math.ceil(ingredient.amount).toInt))
    case event: MouseEntered if event.source == this => borderPainted = true
    case event: MouseExited if event.source == this => borderPainted = false
  }
}

