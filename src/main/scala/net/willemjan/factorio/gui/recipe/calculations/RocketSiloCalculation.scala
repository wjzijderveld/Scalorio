package net.willemjan.factorio.gui.recipe.calculations

import java.util.concurrent.TimeUnit
import javax.swing.ImageIcon

import net.willemjan.factorio.calculator.{CalculationResult, Calculator, Module, Modules}
import net.willemjan.factorio.gui.RecipeSearch
import net.willemjan.factorio.gui.recipe.calculations.event.IngredientClicked
import net.willemjan.factorio.model.{Ingredient, Item, Recipe}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.swing._
import scala.swing.event._

class RocketSiloCalculation(controller: RecipeSearch, item: Item, recipe: Recipe)(implicit val calculator: Calculator) extends BorderPanel with CalculationPanel {

  final val DefaultItemCount = 100
  final val DefaultDuration = FiniteDuration(1, "minute")
  final val DefaultSpeedModuleCount = 8
  final val DefaultProductivityModuleCount = 4

  val durationOptions = Seq("second", "minute", "hour")

  def setAmountAndTimeUnit(amount: Int, timeUnit: TimeUnit): Unit = {
    amountField.text = amount.toString
    durationField.setTimeUnit(timeUnit)
  }

  object amountField extends TextField {
    text = DefaultItemCount.toString
    columns = 2
    def getInt: Int = text match {
      case "" => 1
      case s => s.toInt
    }
  }
  object durationField extends ComboBox(durationOptions) {
    def toDuration: Duration = FiniteDuration(1, selection.item)
    def setTimeUnit(timeUnit: TimeUnit): Unit = selection.item = timeUnit match {
      case TimeUnit.MINUTES => "minute"
      case TimeUnit.HOURS => "hour"
      case _ => "second"
    }
    setTimeUnit(DefaultDuration.unit)
  }
  object speedModuleCountField extends TextField {
    text = DefaultItemCount.toString
    columns = 2
    def getInt: Int = text match {
      case "" => DefaultSpeedModuleCount
      case s => s.toInt
    }
  }
  object productivityModuleCountField extends TextField {
    text = DefaultItemCount.toString
    columns = 2
    def getInt: Int = text match {
      case "" => DefaultProductivityModuleCount
      case s => s.toInt
    }
  }
  object configPanel extends FlowPanel {
    contents.append(
      amountField,
      new Label(s"${item.name}(s) per"),
      durationField,
      new Label("and with"),
      speedModuleCountField,
      new Label("speed modules and"),
      productivityModuleCountField,
      new Label("productivity modules.")
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
  object calculationPanel extends BoxPanel(Orientation.Horizontal) {
    contents += new BoxPanel(Orientation.Vertical) {
      contents += requiredSilosLabel
      contents += requiredEnergyLabel
    }

    contents += new BoxPanel(Orientation.Vertical) {
      def drawIngredients(ingredients: Seq[Ingredient]): Unit = ingredients.foreach {
        ingredient =>
          val label = IngredientLabel(ingredient)
          listenTo(label)
          contents += label
      }

      listenTo(RocketSiloCalculation.this)

      reactions += {
        case CalculationUpdated(_, _, ingredients) =>
          contents.clear()
          drawIngredients(ingredients)
        case e: IngredientClicked => controller.changeItem(e.item, e.amount, durationField.toDuration.unit)
      }
    }

    contents += Swing.VGlue
  }

  listenTo(amountField, durationField.selection, calculationPanel, speedModuleCountField, productivityModuleCountField)

  reactions += {
    case ValueChanged(`amountField`) | SelectionChanged(`durationField`) | ValueChanged(`speedModuleCountField`) | ValueChanged(`productivityModuleCountField`) =>
      val result: CalculationResult = calculator.calculateRocketBuilding(
        item,
        recipe,
        amountField.getInt,
        durationField.toDuration,
        Modules(
          Module.speed(3)(speedModuleCountField.getInt),
          Module.productivity(3)(productivityModuleCountField.getInt)
        ))
      publish(CalculationUpdated(result.crafters, result.energyRequired, result.ingredients))
    case e: IngredientClicked => publish(e)
  }

  layout(configPanel) = BorderPanel.Position.North
  layout(calculationPanel) = BorderPanel.Position.Center

  val result: CalculationResult = calculator.calculateRocketBuilding(item, recipe, amountField.getInt, durationField.toDuration)
  publish(CalculationUpdated(result.crafters, result.energyRequired, result.ingredients))
}


