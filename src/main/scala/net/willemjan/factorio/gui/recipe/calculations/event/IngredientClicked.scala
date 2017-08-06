package net.willemjan.factorio.gui.recipe.calculations.event

import scala.swing.event._

case class IngredientClicked(item: String, amount: Int) extends Event

case class ChangeItemRequested() extends Event
