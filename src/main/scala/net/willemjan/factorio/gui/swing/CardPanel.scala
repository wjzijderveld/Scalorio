package net.willemjan.factorio.gui.swing

import java.awt.CardLayout

import scala.swing._

class CardPanel extends Panel with LayoutContainer {
  type Constraints = String
  def layoutManager = peer.getLayout.asInstanceOf[CardLayout]
  override lazy val peer = new javax.swing.JPanel(new CardLayout) with SuperMixin

  private var cards : Map[String, Component] = Map.empty

  protected def areValid(c: Constraints) = (true, "")
  protected def add(comp: Component, l: Constraints) = {
    // we need to remove previous components with the same constraints as the new one,
    // otherwise the layout manager loses track of the old one
    cards.get(l).foreach { old => cards -= l; peer.remove(old.peer) }
    cards += (l -> comp)
    peer.add(comp.peer, l)
  }

  def show(l : Constraints) = layoutManager.show(peer, l)
  protected def constraintsFor(comp: Component) = cards.iterator.find { case (_, c) => c eq comp}.map(_._1).orNull
}
