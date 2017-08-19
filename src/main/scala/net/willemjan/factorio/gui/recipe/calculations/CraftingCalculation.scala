package net.willemjan.factorio.gui.recipe.calculations

import java.util.concurrent.TimeUnit

import net.willemjan.factorio.calculator.{CalculationResult, Calculator, Module, Modules}
import net.willemjan.factorio.gui.RecipeSearch
import net.willemjan.factorio.gui.recipe.calculations.event.IngredientClicked
import net.willemjan.factorio.model.{AssemblingMachine, Ingredient, Item, Recipe}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.swing._
import scala.swing.event._

class CraftingCalculation(controller: RecipeSearch, item: Item, recipe: Recipe, assemblers: Seq[AssemblingMachine])(implicit val calculator: Calculator) extends BoxPanel(Orientation.Vertical) with CalculationPanel {

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
      case "" => DefaultItemCount
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
  object speedModuleCountField extends TextField {
    text = DefaultItemCount.toString
    columns = 2
    def getInt: Int = text match {
      case "" => DefaultItemCount
      case s => s.toInt
    }
  }
  object productivityModuleCountField extends TextField {
    text = DefaultItemCount.toString
    columns = 2
    def getInt: Int = text match {
      case "" => DefaultItemCount
      case s => s.toInt
    }
  }
  object configPanel extends FlowPanel(FlowPanel.Alignment.Leading)() {
    contents.append(
      amountField,
      new Label(s"${item.name}(s) per"),
      durationField,
      new Label("using"),
      assemblerField,
      new Label("and with"),
      speedModuleCountField,
      new Label("speed modules and"),
      productivityModuleCountField,
      new Label("productivity modules.")
    )
  }
  object requiredAssemblers extends Label {
    horizontalAlignment = Alignment.Left
    horizontalTextPosition = Alignment.Right
    listenTo(CraftingCalculation.this)

    reactions += {
      case CalculationUpdated(amount, _, _) =>
        println("Reacting with label")
        text = s"$amount ${assemblerField.selection.item}(s)"
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
        ingredient =>
          val label = IngredientLabel(ingredient)
          listenTo(label)
          contents += label
      }

      listenTo(CraftingCalculation.this)

      reactions += {
        case CalculationUpdated(_, _, ingredients) =>
          println("Reacting to CalcuationUpdated")
          contents.clear()
          drawIngredients(ingredients)
        case e: IngredientClicked =>
          println(s"clicked $e")
          controller.changeItem(e.item, e.amount, durationField.toDuration.unit)
      }
    }

    contents += Swing.VGlue
  }

  listenTo(amountField, durationField.selection, assemblerField.selection, speedModuleCountField, productivityModuleCountField)

  def triggerCalculation(): Unit = {
    val result: CalculationResult = calculator.calculateCrafting(
      item,
      recipe,
      assemblerField.getAssembler,
      amountField.getInt,
      durationField.toDuration,
      Modules(
        Module.speed(3)(speedModuleCountField.getInt),
        Module.speed(3)(productivityModuleCountField.getInt)
      )
    )
    println("Publishing", result)
    publish(CalculationUpdated(result.crafters, result.energyRequired, result.ingredients))
  }

  reactions += {
    case e: EditDone if e.source == amountField =>
      println("Publishing after change WTF", amountField.getInt, durationField.toDuration.unit)
      triggerCalculation()
    case SelectionChanged(`durationField`) => triggerCalculation()
    case _: CalculationUpdated => println("Update received")
  }

  contents += configPanel
  contents += calculationPanel
//  layout(configPanel) = BorderPanel.Position.North
//  layout(calculationPanel) = BorderPanel.Position.Center

  triggerCalculation()
}
