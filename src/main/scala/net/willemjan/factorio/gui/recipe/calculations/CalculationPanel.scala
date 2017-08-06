package net.willemjan.factorio.gui.recipe.calculations

import java.util.concurrent.TimeUnit

import scala.swing.{Panel, Publisher}

trait CalculationPanel extends Panel with Publisher {
  def setAmountAndTimeUnit(amount: Int, timeUnit: TimeUnit): Unit
}
