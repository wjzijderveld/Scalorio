package net.willemjan.factorio.gui.recipe

import java.util.concurrent.TimeUnit
import javax.swing.ImageIcon

import net.willemjan.factorio.calculator.{CalculationResult, Calculator}
import net.willemjan.factorio.model.{Ingredient, Item, Recipe}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.swing.Swing.EmptyIcon
import scala.swing._
import scala.swing.event._

class RocketSiloCalculation(item: Item, recipe: Recipe)(implicit val calculator: Calculator) extends BorderPanel with Publisher {

  final val DefaultItemCount = 100
  final val DefaultDuration = FiniteDuration(1, "minute")

  val durationOptions = Seq("second", "minute", "hour")

  object amountField extends TextField {
    text = DefaultItemCount.toString
    columns = 2
    def getInt: Int = text match {
      case "" => 1
      case s => s.toInt
    }
  }
  object durationField extends ComboBox(durationOptions) {
    selection.item = DefaultDuration.unit match {
      case TimeUnit.MINUTES => "minute"
      case TimeUnit.HOURS => "hour"
      case _ => "second"
    }
    def toDuration: Duration = FiniteDuration(1, selection.item)
  }
  object configPanel extends FlowPanel {
    contents.append(
      amountField,
      new Label(s"${item.name}(s) per"),
      durationField
    )
  }
  object requiredSilosLabel extends Label {
    horizontalAlignment = Alignment.Left
    horizontalTextPosition = Alignment.Right
    listenTo(RocketSiloCalculation.this)

    reactions += {
      case CalculationUpdated(amount, _, _) => text = s"$amount rocket silo(s)"
    }
  }
  object requiredEnergyLabel extends Label {
    horizontalAlignment = Alignment.Left
    horizontalTextPosition = Alignment.Right
    listenTo(RocketSiloCalculation.this)

    reactions += {
      case CalculationUpdated(_, energyRequired, _) => text = f"$energyRequired%2.1f kWh"
    }
  }
  case class IngredientLabel(ingredient: Ingredient) extends Label {
    text = f"${ingredient.amount}%2.2f x ${ingredient.name}"
    horizontalTextPosition = Alignment.Right
    horizontalAlignment = Alignment.Left
    icon = ingredient.icon match {
      case None => EmptyIcon
      case Some(icon) => new ImageIcon(icon)
    }
  }
  object calculationPanel extends BoxPanel(Orientation.Horizontal) {
    contents += new BoxPanel(Orientation.Vertical) {
      contents += requiredSilosLabel
      contents += requiredEnergyLabel
    }

    contents += new BoxPanel(Orientation.Vertical) {
      def drawIngredients(ingredients: Seq[Ingredient]): Unit = ingredients.foreach {
        ingredient => contents += IngredientLabel(ingredient)
      }

      listenTo(RocketSiloCalculation.this)

      reactions += {
        case CalculationUpdated(_, _, ingredients) =>
          contents.clear()
          drawIngredients(ingredients)
      }
    }

    contents += Swing.VGlue
  }

  listenTo(amountField, durationField.selection)

  reactions += {
    case ValueChanged(`amountField`) | SelectionChanged(`durationField`) =>
      val result: CalculationResult = calculator.calculateRocketBuilding(item, recipe, amountField.getInt, durationField.toDuration)
      publish(CalculationUpdated(result.crafters, result.energyRequired, result.ingredients))
  }

  layout(configPanel) = BorderPanel.Position.North
  layout(calculationPanel) = BorderPanel.Position.Center

  val result: CalculationResult = calculator.calculateRocketBuilding(item, recipe, amountField.getInt, durationField.toDuration)
  publish(CalculationUpdated(result.crafters, result.energyRequired, result.ingredients))
}

case class CalculationUpdated(buildCount: Double, energyRequired: Double = 0.0, ingredientsRequired: Seq[Ingredient] = Seq.empty) extends Event
