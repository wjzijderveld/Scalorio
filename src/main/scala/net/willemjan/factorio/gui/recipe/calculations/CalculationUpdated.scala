package net.willemjan.factorio.gui.recipe.calculations

import net.willemjan.factorio.model.Ingredient

import scala.swing.event.Event

case class CalculationUpdated(buildCount: Double, energyRequired: Double = 0.0, ingredientsRequired: Seq[Ingredient] = Seq.empty) extends Event
