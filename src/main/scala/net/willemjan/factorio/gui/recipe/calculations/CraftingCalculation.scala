package net.willemjan.factorio.gui.recipe.calculations

import java.util.concurrent.TimeUnit

import net.willemjan.factorio.calculator.{CalculationResult, Calculator}
import net.willemjan.factorio.model.{AssemblingMachine, Ingredient, Item, Recipe}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.swing._
import scala.swing.event._

class CraftingCalculation(item: Item, recipe: Recipe, assemblers: Seq[AssemblingMachine])(implicit val calculator: Calculator) extends BorderPanel with CalculationPanel {

  final val DefaultItemCount = 1
  final val DefaultDuration = FiniteDuration(1, "second")

  val durationOptions = Seq("second", "minute", "hour")

  def setAmountAndTimeUnit(amount: Int, timeUnit: TimeUnit): Unit = {
    println("Setting amount and duration")
    amountField.text = amount.toString
    durationField.setTimeUnit(timeUnit)
  }

  object amountField extends TextField {
    text = DefaultItemCount.toString
    columns = 5
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
  object assemblerField extends ComboBox(assemblers.filter(_.categories.contains(recipe.category)).map(_.name)) {
    def getAssembler: AssemblingMachine = assemblers.find(_.name == selection.item).get
  }
  object configPanel extends FlowPanel {
    contents.append(
      amountField,
      new Label(s"${item.name}(s) per"),
      durationField,
      new Label("using"),
      assemblerField
    )
  }
  object requiredAssemblers extends Label {
    horizontalAlignment = Alignment.Left
    horizontalTextPosition = Alignment.Right
    listenTo(CraftingCalculation.this)

    reactions += {
      case CalculationUpdated(amount, _, _) =>
        println("Reacting with label")
        text = s"$amount assembler(s)"
    }
  }
  object requiredEnergyLabel extends Label {
    horizontalAlignment = Alignment.Left
    horizontalTextPosition = Alignment.Right
    listenTo(CraftingCalculation.this)

    reactions += {
      case CalculationUpdated(_, energyRequired, _) => text = f"$energyRequired%2.1f kWh"
    }
  }
  object calculationPanel extends BoxPanel(Orientation.Horizontal) {
    contents += new BoxPanel(Orientation.Vertical) {
      contents += requiredAssemblers
      contents += requiredEnergyLabel
    }

    contents += new BoxPanel(Orientation.Vertical) {
      def drawIngredients(ingredients: Seq[Ingredient]): Unit = ingredients.foreach {
        ingredient => contents += IngredientLabel(ingredient)
      }

      listenTo(CraftingCalculation.this)

      reactions += {
        case CalculationUpdated(_, _, ingredients) =>
          println("Reacting to CalcuationUpdated")
          contents.clear()
          drawIngredients(ingredients)
      }
    }

    contents += Swing.VGlue
  }

  listenTo(amountField, durationField.selection, assemblerField.selection)

  def triggerCalculation(): Unit = {
    val result: CalculationResult = calculator.calculateCrafting(item, recipe, assemblerField.getAssembler, amountField.getInt, durationField.toDuration)
    println("Publishing", result)
    publish(CalculationUpdated(result.crafters, result.energyRequired, result.ingredients))
  }

  reactions += {
    case ValueChanged(`amountField`) | SelectionChanged(`durationField`) =>
      println("Publishing after change", amountField.getInt, durationField.toDuration.unit)
      triggerCalculation
    case _: CalculationUpdated => println("Update received")
  }

  layout(configPanel) = BorderPanel.Position.North
  layout(calculationPanel) = BorderPanel.Position.Center

  triggerCalculation
}
